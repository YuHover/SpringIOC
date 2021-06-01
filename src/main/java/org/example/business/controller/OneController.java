package org.example.business.controller;

import org.example.business.service.OneService;
import org.example.spring.Autowired;
import org.example.spring.Component;

@Component
public class OneController {
    @Autowired
    private OneService oneService;

    public OneController() {
    }

    public OneService getOneService() {
        return oneService;
    }

    public void setOneService(OneService oneService) {
        this.oneService = oneService;
    }

    @Override
    public String toString() {
        return "OneController{" +
                "oneService=" + oneService +
                '}';
    }
}
