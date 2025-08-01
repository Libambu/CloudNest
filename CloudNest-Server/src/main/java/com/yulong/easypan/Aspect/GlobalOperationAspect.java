package com.yulong.easypan.Aspect;

import com.yulong.easypan.annotation.GlobalInterceptor;
import com.yulong.easypan.entity.constants.Constants;
import com.yulong.easypan.entity.dto.SessionWebUserDto;
import com.yulong.easypan.entity.enums.ResponseCodeEnum;
import com.yulong.easypan.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect{
    //接下来定义一个切点

    @Pointcut("@annotation(com.yulong.easypan.annotation.GlobalInterceptor)")
    private void requestInterceptor(){

    }
    //接下来定义是在切入点之前执行代码还是之后执行，然后正常写
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) throws BusinessException, NoSuchMethodException {
        //然后参数校验的逻辑就在这里写就o了
        try{
            //获得当前类,切记不是写在注解下的方法
            Object target = point.getTarget();
            //获得当前方法的参数
            Object[] argments = point.getArgs();
            //获取当前方法的名称
            String methodname = point.getSignature().getName();
            //获取当前方法的参数属性
            Class<?>[] parameterTypes = ((MethodSignature)point.getSignature()).getMethod().getParameterTypes();
            //然后就可以获得注解类了，为什么还要参数属性呢？是防止有重载的情况
            Method method = target.getClass().getMethod(methodname,parameterTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);

            if(interceptor==null){
                return;
            }

            /**
             * 校验登录
             */
            if(interceptor.checklogin() == true||interceptor.checkAdmin()==true){
                //开始登录校验
                checklogin(interceptor.checkAdmin());
            }

        }catch(Exception e){
            log.error("全局拦截器异常", e);
            throw e;
        }
    }

    /**
     * @param checkAdmin
     */
    private void checklogin(boolean checkAdmin){
        //在AOP中获取SESSION对象
        HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto sessionWebUserDto =(SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
        if(sessionWebUserDto == null){
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if(checkAdmin&&!sessionWebUserDto.getIsAdmin()){
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
    }
}
