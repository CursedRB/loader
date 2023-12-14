package pw.geckonerd.cursedrbloader;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import javassist.ClassPool;
import javassist.CtClass;

public class CursedInjectionTransformer implements ClassFileTransformer {
	String targetClassName;
	ClassLoader targetClassLoader;
	CursedInjection handler;
	
    public CursedInjectionTransformer(String name, ClassLoader classLoader, CursedInjection handler){
		this.targetClassLoader = classLoader;
		this.targetClassName = name;
		this.handler = handler;
	}

	@Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        byte[] byteCode = classfileBuffer;
        String finalTargetClassName = this.targetClassName.replaceAll("\\.", "/"); 
        if (!className.equals(finalTargetClassName)) {
        	return byteCode;
        }

        if (className.equals(finalTargetClassName) && loader.equals(targetClassLoader)) {
        	try {
        		ClassPool cp = ClassPool.getDefault();
        		CtClass cc = cp.get(targetClassName);
        		
        		this.handler.transform(cp, cc);
        		
                byteCode = cc.toBytecode();
                cc.detach();
            } catch (Exception e) {
               e.printStackTrace();
            }
        }
        return byteCode;
	}
}
