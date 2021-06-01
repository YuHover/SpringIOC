package org.example;


import org.example.spring.AnnotationConfigApplicationContext;


public class App {
    public static void main( String[] args ) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(RootConfig.class);

        Object c1 = context.getBean("oneController");
        Object c2 = context.getBean("oneController");
        System.out.println(c1 == c2);

        Object s1 = context.getBean("oneService");
        Object s2 = context.getBean("oneService");
        System.out.println(s1 == s2);

        Object d1 = context.getBean("oneDao");
        Object d2 = context.getBean("oneDao");
        System.out.println(d1 == d2);

        Object x = context.getBean("NotExists");
        System.out.println(x);
    }
}
