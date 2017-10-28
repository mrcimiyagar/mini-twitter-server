package models.resolvers.base;

import models.network.NetClient;

public abstract class BaseResolver {

    public abstract void resolvePacket(NetClient netClient, Object packet);
}