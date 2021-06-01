package org.example.business.dao;


import org.example.business.controller.OneController;
import org.example.spring.Autowired;
import org.example.spring.Component;
import org.example.spring.Scope;
import org.example.spring.ScopeType;

@Component
@Scope(ScopeType.PROTOTYPE)
public class OneDao {
    @Autowired
    private OneController oneController;

    public OneDao() {
    }

    public OneController getOneController() {
        return oneController;
    }

    public void setOneController(OneController oneController) {
        this.oneController = oneController;
    }
}
