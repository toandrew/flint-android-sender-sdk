package tv.matchstick.server.fling;

import java.nio.ByteBuffer;

public interface IController {
    void connectToDeviceInternal();
    void setVolumeInternal(double level, boolean mute);
    void onSocketConnectionFailedInternal(int socketError);
    void stopApplicationInternal();
    void joinApplicationInternal(String url);
    void sendTextMessage(String namespace, String message, long id,
            String transportId);
    void launchApplicationInternal(String applicationId,
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
    void reconnectToDevice(String lastAppId);
    void connectDevice();
    void launchApplication(String applicationId,
            boolean relaunchIfRunning);
    void joinApplication(String url);
    void leaveApplication();
    void stopApplication();
    void requestStatus();
    void setVolume(double volume, boolean mute);
    void sendMessageInternal(String namespace, String message, long requestId);
    void setMessageReceivedCallbacks(String namespace);
    void removeMessageReceivedCallbacks(String namespace);
}
