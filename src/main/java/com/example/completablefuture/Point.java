package com.example.completablefuture;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author zengxc
 * @Date 2018/12/10
 */

public class Point {

    private Integer x;
    private Integer y;

    public Point() {
    }

    public Point(Integer x, Integer y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }

    private static void setProperty(Point point, String proName) throws IntrospectionException, InvocationTargetException, IllegalAccessException {
        PropertyDescriptor descriptor = new PropertyDescriptor(proName, Point.class);
        Method setMethod = descriptor.getWriteMethod();
        setMethod.invoke(point, 8);
        System.out.println(point.toString());
    }

    private static void getProperty(Point point, String proName) throws Exception{
        PropertyDescriptor descriptor = new PropertyDescriptor(proName, Point.class);
        Method getMethod = descriptor.getReadMethod();
        Object invoke = getMethod.invoke(point);
        System.out.println(invoke);
    }

    public static void main(String[] args) throws Exception {
        Point point = new Point();
        setProperty(point, "x");

        getProperty(point, "x");
    }
}
