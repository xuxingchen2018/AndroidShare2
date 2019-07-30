package com.mrcd.apt.learn;

import java.lang.reflect.Method;

/**
 * findView的统一入口
 */
public class ViewFinder {

    private static final ViewFinder INSTANCE = new ViewFinder();

    public static ViewFinder getInstance() {
        return INSTANCE;
    }

    private ViewFinder() {
    }

    public void inject(Object object) {
        //此处可以酌情优化，eg:做一个缓存，缓存固定数量的finder类
        if (object != null) {
            String className = object.getClass().getName() + this.getClass().getSimpleName();
            try {
                Class<?> aClass = Class.forName(className);
                Object instance = aClass.newInstance();
                Method bindView = aClass.getDeclaredMethod("bindView", object.getClass());
                bindView.invoke(instance, object);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
