package models.packets.base;

public abstract class BaseRequest {

    public long packetCode;

    public abstract int getRequestCode();
}