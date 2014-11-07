package tv.matchstick.server.fling;

import java.nio.ByteBuffer;

public interface IController {
    void connectToDeviceInternal();
    void setVolumeInternal(double level, boolean mute);
    void onSocketConnectionFailedInternal(int socketError);
    void stopApplicationInternal(String sessionId);
    void joinApplicationInternal(String applicationId, String sessionId);
    void sendTextMessage(String namespace, String message, long id,
            String transportId);
    void launchApplicationInternal(String applicationId, String param,
            boolean relaunch);
    void onReceivedMessage(ByteBuffer message);
    void onReceivedMessage(String message);
    void leaveApplicationInternal();
    void onSocketDisconnectedInternal(int socketError);
    void addNamespace(String namespace);
    void getStatus();
    void removeNamespace(String namespace);
    void onSocketConnectedInternal();
    void generateId();
    void releaseReference();
    boolean isDisposed();
    boolean isConnected();
    boolean isConnecting();
    void reconnectToDevice(String lastAppId, String sessionId);
    void connectDevice();
    void launchApplication(String applicationId, String param,
            boolean relaunchIfRunning);
    void joinApplication(String applicationId, String sessionId);
    void leaveApplication();
    void stopApplication(String sessionId);
    void requestStatus();
    void setVolume(double volume, boolean mute);
    void sendMessageInternal(String namespace, String message, long requestId);
    void setMessageReceivedCallbacks(String namespace);
    void removeMessageReceivedCallbacks(String namespace);
}
