package com.mrcd.apt.compiler;

import com.google.auto.service.AutoService;
import com.mrcd.apt.annotation.BindView;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

//@SupportedOptions({"key"})//此处的注解是增加编译时的一些配置参数用的，如ARouter在使用时就需要外部传入Project.name的属性
@AutoService(Processor.class)//表明是一个注解处理器
@SupportedSourceVersion(SourceVersion.RELEASE_7)//支持的Java源代码版本
@SupportedAnnotationTypes({BindViewProcessor.SUPPORT_ANNOTATION_TYPE})//支持的注解类型，是一个数组，一个处理器可以处理多个注解
public class BindViewProcessor extends AbstractProcessor {

    public static final String SUPPORT_ANNOTATION_TYPE = "com.mrcd.apt.annotation.BindView";

    public static final String VIEW_FINDER_SUFFIX = "ViewFinder";

    private Filer mFiler;
    private Elements mElementUtils;

    //存储activity和View的信息，key为activity类，value为收集到的注解view信息
    private Map<TypeElement, Set<ViewInfo>> mBindViews = new HashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = super.getSupportedAnnotationTypes();
        System.out.println("support size >>> "+supportedAnnotationTypes.size());
        return supportedAnnotationTypes;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("start process");
        return processBindViewAnnotation(set, roundEnvironment);
    }

    private boolean processBindViewAnnotation(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (null != set && set.size() > 0) {
            Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
            if (null != elements && elements.size() > 0) {
                System.out.println("BindView count: " + elements.size());
                categories(elements);
                Set<TypeElement> typeElements = mBindViews.keySet();
                for (TypeElement element : typeElements) {
                    JavaFile javaFile = generateViewFinder(element, mBindViews.get(element));
                    try {
                        javaFile.writeTo(mFiler);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("finish process");
                return true;
            }
        }
        System.out.println("no process");
        return false;
    }

    /**
     * 生成一个全新的FindView的Java文件
     *
     * @param element   类型element  包括class，interface
     * @param viewInfos 此类中所有的注解的view的信息
     * @return
     */
    private JavaFile generateViewFinder(TypeElement element, Set<ViewInfo> viewInfos) {
        System.out.println("View info size: " + viewInfos.size());
        MethodSpec.Builder builder = MethodSpec.methodBuilder("bindView")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addParameter(ClassName.get(element.asType()), "target")
            .returns(void.class);
        builder.beginControlFlow("if (null != target)");
        for (ViewInfo info : viewInfos) {
            String format = "target.%s = target.findViewById(%d)";
            builder.addStatement(String.format(Locale.US, format, info.mName, info.mId));
        }
        builder.endControlFlow();
        TypeSpec.Builder finderBuilder = TypeSpec.classBuilder(getSimpleClassName(element) + VIEW_FINDER_SUFFIX)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(builder.build());
        JavaFile javaFile = JavaFile.builder(getPackage(element), finderBuilder.build()).build();
        return javaFile;
    }

    /**
     * 将所有BindView的注解进行分类
     *
     * @param elements 所有的注解集合
     */
    private void categories(Set<? extends Element> elements) {
        for (Element element : elements) {  //遍历每一个element
            VariableElement variableElement = (VariableElement) element;    //被@BindView标注的应当是变量，这里简单的强制类型转换
            TypeElement enclosingElement = (TypeElement) variableElement.getEnclosingElement(); //获取代表Activity的TypeElement
            Set<ViewInfo> views = mBindViews.get(enclosingElement); //views储存着一个Activity中将要绑定的view的信息
            if (views == null) {    //如果views不存在就new一个
                views = new HashSet<>();
                mBindViews.put(enclosingElement, views);
            }
            BindView bindAnnotation = variableElement.getAnnotation(BindView.class);    //获取到一个变量的注解
            int id = bindAnnotation.value();    //取出注解中的value值，这个值就是这个view要绑定的xml中的id
            views.add(new ViewInfo(variableElement.getSimpleName().toString(), id));    //把要绑定的View的信息存进views中
        }
    }

    /**
     * 获取包名的方法
     *
     * @param element TypeElement
     * @return 包名路径
     */
    String getPackage(Element element) {
        return mElementUtils.getPackageOf(element).getQualifiedName().toString();
    }

    /**
     * 获取类名的方法，不包含包名
     *
     * @param element TypeElement
     * @return 类名
     */
    String getSimpleClassName(Element element) {
        return element.getSimpleName().toString();
    }

    /**
     * 被注解的view
     */
    static class ViewInfo {

        String mName;

        int mId;

        public ViewInfo(String name, int id) {
            mName = name;
            mId = id;
        }
    }
}
