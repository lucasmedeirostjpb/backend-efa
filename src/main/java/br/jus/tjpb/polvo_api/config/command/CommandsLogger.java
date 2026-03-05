package br.jus.tjpb.polvo_api.config.command;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CommandsLogger {
    private static final Logger log = LoggerFactory.getLogger(CommandsLogger.class);

    @Before("execution(* br.jus.tjpb.polvo_api.application..*CommandHandler.handle(..)) && args(command,..)")
    public void logCommand(JoinPoint joinPoint, AbstractCommand command) {
        String userId = command.getUser() != null ? command.getUser().id() : "SISTEMA";
        log.info("Executando commando: {} por usuário: {}", joinPoint.getSignature().getName(), userId);
    }
}
