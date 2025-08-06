package me.kall.dimban.api;

public interface IServerLevel {
    boolean dimBan$isBlacklisted();
    void dimBan$setBlacklisted(boolean blacklisted);
}
