package org.example.business.service;

import org.example.business.dao.OneDao;
import org.example.spring.Autowired;
import org.example.spring.Component;

@Component
public class OneService {

    @Autowired
    private OneDao oneDao;

    public OneService() {
    }

    public OneDao getOneDao() {
        return oneDao;
    }

    public void setOneDao(OneDao oneDao) {
        this.oneDao = oneDao;
    }

    @Override
    public String toString() {
        return "OneService{" +
                "oneDao=" + oneDao +
                '}';
    }
}
