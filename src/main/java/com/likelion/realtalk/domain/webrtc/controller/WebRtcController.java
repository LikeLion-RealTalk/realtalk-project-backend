package com.likelion.realtalk.domain.webrtc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebRtcController {

  @GetMapping("/test/webrtc")
  public String webrtc() {
    System.out.println("WebRtcController.webrtc");
    return "webrtc"; // returns the name of the HTML file (webrtc.html) in the templates directory
  }

}
