package org.example.chessmystic.Controller;

import org.example.chessmystic.Models.UIUX.TieResolutionOption;
import org.example.chessmystic.Service.implementation.GameRelated.TieResolutionOptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tie-resolution")
public class TieResolutionController {

    @Autowired
    private TieResolutionOptionService service;

    @GetMapping("/options")
    public List<TieResolutionOption> getAllOptions() {
        return service.getAllOptions();
    }


}