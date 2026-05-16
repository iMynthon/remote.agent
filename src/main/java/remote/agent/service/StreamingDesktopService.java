package remote.agent.service;

import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.Cancellable;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для удалённого захвата экрана и передачи видеопотока через WebSocket.
 * Использует реактивные стримы Mutiny для непрерывной отправки кадров.
 * <p>
 * Основные функции:
 * - Однократное получение скриншота (desktopRemoteScreen)
 * - Запуск/остановка непрерывного видеопотока (startStreamingRemoteScreen / stopStreamingRemoteScreen)
 * </p>
 */
@Slf4j                        // Lombok: создаёт логгер log
@ApplicationScoped           // Один экземпляр на всё приложение (CDI-бин)
public class StreamingDesktopService {

    /**
     * Хранилище активных стримов: ключ = connectionId (идентификатор клиента WebSocket),
     * значение = Cancellable, с помощью которого можно остановить поток кадров.
     */
    private final Map<String, Cancellable> activeStreams = new ConcurrentHashMap<>();

    /**
     * Запускает непрерывную отправку скриншотов (видеопоток) клиенту с указанным connectionId.
     * Кадры отправляются каждые 100 мс (10 кадров в секунду).
     * <p>
     * При повторном вызове для того же connectionId старый стрим будет остановлен перед запуском нового.
     * </p>
     *
     * @param webSocketClientConnection – соединение WebSocket, через которое отправлять кадры
     * @param connectionId        – идентификатор клиента (для логирования и управления)
     * @return Uni<Void> – сигнал завершения запуска (в данном случае просто пустой)
     */
    public Uni<Void> startStreamingRemoteScreen(WebSocketClientConnection webSocketClientConnection, String connectionId) {
        log.info("Callable startStreamingRemoteScreen: connectionId={}", connectionId);
        // Останавливаем предыдущий стрим для этого же клиента, если он существует
        // .subscribe().with() – подписываемся на результат остановки (асинхронно)
        stopStreamingRemoteScreen(connectionId).subscribe().with(
                // Успешная остановка
                success -> log.info("Old stream for client {} stopped before restarting", connectionId),
                // Ошибка при остановке
                failure -> log.info("Failed to stop old stream for client: connectionId {}, error message: {}", connectionId, failure.getMessage())
        );
        // Создаём реактивный поток (Multi), который каждые 100 мс генерирует тик
        // .ticks().every(Duration.ofMillis(100)) – генерирует бесконечный поток чисел (0,1,2,...) с интервалом 100 мс
        Cancellable cancellable = Multi.createFrom().ticks().every(Duration.ofMillis(250))
                // Для каждого тика (startSt) вызываем desktopRemoteScreen() (который возвращает Uni<byte[]>)
                // transformToUniAndConcatenate – каждый входящий элемент преобразуется в Uni, и результаты склеиваются в один поток
                .onItem().transformToUniAndConcatenate(startSt ->
                        desktopRemoteScreen()
                                // Полученный Uni<byte[]> (скриншот) преобразуем в Uni<Void> путём отправки по WebSocket
                                .onItem().transformToUni(webSocketClientConnection::sendBinary)
                )
                // Подписываемся на полученный поток (запускаем его)
                .subscribe().with(
                        // onNext – при каждой успешной отправке кадра
                        success -> log.info("Frame sent to {}", connectionId),
                        // onError – если произошла ошибка в потоке
                        failure -> {
                            log.info("Stream error for {}", connectionId, failure);
                            // При ошибке останавливаем стрим, чтобы не плодить мёртвые подписки
                            stopStreamingRemoteScreen(connectionId).subscribe().with(
                                    s -> log.info("Stopped stream after error for {}", connectionId),
                                    f -> log.info("Failed to stop stream after error for {}", connectionId, f)
                            );
                        }
                );
        // Сохраняем объект Cancellable в карту, чтобы потом можно было остановить стрим по connectionId
        activeStreams.put(connectionId, cancellable);
        // Возвращаем пустой Uni (завершённый), чтобы вызывающий код мог дождаться окончания запуска (хотя мы ничего не возвращаем)
        return Uni.createFrom().voidItem();
    }

    /**
     * Останавливает видеопоток для указанного клиента.
     * Если активного стрима нет – ничего не делает.
     *
     * @param connectionId – идентификатор клиента, для которого нужно остановить отправку кадров
     * @return Uni<Void> – сигнал завершения операции остановки
     */
    public Uni<Void> stopStreamingRemoteScreen(String connectionId) {
        // Извлекаем Cancellable из карты и удаляем запись
        Cancellable cancellable = activeStreams.remove(connectionId);
        if (cancellable != null) {
            // Отменяем подписку – это остановит генерацию тиков и отправку кадров
            cancellable.cancel();
            // Возвращаем завершённый Uni
            return Uni.createFrom().voidItem();
        }
        // Если стрима не было – тоже возвращаем завершённый Uni
        return Uni.createFrom().voidItem();
    }

    /**
     * Делает один скриншот рабочего стола и возвращает его в виде массива байт (JPEG).
     * Работает в блокирующем режиме, поэтому обёрнут в Uni.createFrom().item(...) – выполняется на worker-потоке.
     *
     * @return Uni<byte[]> – реактивный контейнер с JPEG-данными
     */
    private Uni<byte[]> desktopRemoteScreen() {
        // Uni.createFrom().item(() -> {...}) – создаёт реактивный элемент,
        // лямбда внутри выполнится на рабочем потоке (не блокируя event-loop)
        log.info("Starting streaming desktop remote screen");
        return Uni.createFrom().item(() -> {
            try {
                // Robot – утилита Java для управления экраном и мышью
                Robot robot = new Robot();
                // Прямоугольник, охватывающий весь экран (размер текущего дисплея)
                Rectangle rectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                // Делаем скриншот
                BufferedImage image = robot.createScreenCapture(rectangle);
                // Поток для записи в массив байт
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                // Кодируем изображение в формат JPEG и записываем в baos
                ImageIO.write(image, "jpeg", baos);
                // Возвращаем массив байт (сжатое изображение)
                return baos.toByteArray();
            } catch (AWTException | IOException e) {
                // Если что-то пошло не так – пробрасываем RuntimeException
                throw new RuntimeException(e);
            }
        });
    }
}
