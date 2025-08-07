package com.likelion.realtalk.global.redis;

public class RedisKeyUtil {

  public static String getRoomKey(String roomId) {
    return "room:" + roomId;
  }

  public static String getExpireKey(String roomId) {
    return "room:" + roomId + ":currentSpeakerExpire";
  }

  public static String getAudienceExpireKey(String roomId) {
    return "room:" + roomId + ":audienceExpire";
  }

  public static String getSpeechesKey(String roomId) {
    return "room:" + roomId + ":speeches";
  }
}