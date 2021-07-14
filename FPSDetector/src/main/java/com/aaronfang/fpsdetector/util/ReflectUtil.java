package com.aaronfang.fpsdetector.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by AaFaa
 * on 2021/7/14
 * in package com.aaronfang.fpsdetector.util
 * with project FpsDetector
 */
public class ReflectUtil {
    public static <T> T reflectObject(Object o, String name) {
        try{
            Field field = o.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T reflectHideObject(Object o, String name) {
        try{
            Method declaredField = Class.class.getDeclaredMethod("getDeclaredField", String.class);
            Field hiddenField = (Field) declaredField.invoke(o.getClass(), name);
            hiddenField.setAccessible(true);
            return (T) hiddenField.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T reflectHideObject(Object o, String name, Class<?>... argTypes) {
        try{
            Method declaredField = Class.class.getDeclaredMethod("getDeclaredField", String.class, Class[].class);
            Field hiddenField = (Field) declaredField.invoke(o.getClass(), name, argTypes);
            hiddenField.setAccessible(true);
            return (T) hiddenField.get(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Method reflectChoreographerMethod(Object instance, String name) {
        return reflectMethodActual(instance.getClass(), name, long.class, Object.class, Object.class);
    }

    public static Method reflectMethodActual(Class clazz, String name, Class<?>... args) {
        try {
            Method declaredField = Class.class.getDeclaredMethod("getDeclaredField", String.class);
            Method hiddenField = (Method) declaredField.invoke(clazz, name, args);
            hiddenField.setAccessible(true);
            return hiddenField;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
