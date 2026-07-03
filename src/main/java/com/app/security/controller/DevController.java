package com.app.security.controller;

import com.app.security.dto.Response;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/dev")
public class DevController {

    @GetMapping("/test")
    public Response<Map<String, Object>> test() {
        Map<String, Object> data = Map.of(
                "status", "ok",
                "service", "pos-cloud-be",
                "version", "1.0.0"
        );
        return new Response<>("Test Dev Api", data, HttpStatus.OK);
    }
}
