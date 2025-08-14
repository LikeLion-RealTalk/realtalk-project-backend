package com.likelion.realtalk.global.redis;

public class RedisKeyUtil {

  public static String getRoomKey(String roomUUID) {
    return "room:" + roomUUID + ":turn";
  }

  public static String getExpireKey(String roomUUID) {
    return "room:" + roomUUID + ":currentSpeakerExpire";
  }

  public static String getAudienceExpireKey(String roomUUID) {
    return "room:" + roomUUID + ":audienceExpire";
  }

  public static String getDebateRoomExpire(String roomUUID) {
    return "room:" + roomUUID + ":debateRoomExpire";
  }

  public static String getSpeechesKey(String roomUUID) {
    return "room:" + roomUUID + ":speeches";
  }

  public static String getAiSummariesKey(String roomUUID) {
    return "room:" + roomUUID + ":aiSummaries";
  }
}