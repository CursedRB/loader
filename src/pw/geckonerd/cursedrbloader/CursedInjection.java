package pw.geckonerd.cursedrbloader;
import javassist.ClassPool;
import javassist.CtClass;

public interface CursedInjection {	
    public abstract void transform(ClassPool pool, CtClass clazz) throws Exception;
}
