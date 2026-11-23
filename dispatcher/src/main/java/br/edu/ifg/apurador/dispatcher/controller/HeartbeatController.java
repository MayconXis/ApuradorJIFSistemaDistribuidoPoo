package br.edu.ifg.apurador.dispatcher.controller;

import static org.springframework.http.HttpStatus.NO_CONTENT;

import br.edu.ifg.apurador.dispatcher.dto.request.HeartbeatReceberRequest;
import br.edu.ifg.apurador.dispatcher.service.HeartbeatReceberService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/interno/heartbeats")
public class HeartbeatController {

    private final HeartbeatReceberService heartbeatReceberService;

    @PostMapping
    @ResponseStatus(NO_CONTENT)
    public void receber(@RequestBody HeartbeatReceberRequest request) {
        heartbeatReceberService.receber(request);
    }
}
