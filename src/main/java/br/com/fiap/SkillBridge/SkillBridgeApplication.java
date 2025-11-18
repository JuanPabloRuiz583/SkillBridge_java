package br.com.fiap.SkillBridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * ============================================================
 *  APLICAÇÃO: SkillBridgeApplication
 * ============================================================
 * Classe principal da aplicação Spring Boot.
 *
 * Responsabilidades:
 * - Ponto de entrada (método main) da aplicação SkillBridge.
 * - Habilita a configuração automática do Spring Boot por meio
 *   da anotação {@link SpringBootApplication}.
 * - Habilita o mecanismo de cache com {@link EnableCaching},
 *   permitindo o uso de anotações como @Cacheable, @CacheEvict, etc.
 *
 * Fluxo de inicialização:
 * - Ao executar o método main:
 *   1) O Spring cria o ApplicationContext.
 *   2) Faz o scan dos componentes (controllers, services, repositories).
 *   3) Sobe o servidor embutido (Tomcat/Jetty) na porta configurada
 *      (por padrão, 8080).
 */
@SpringBootApplication
@EnableCaching
public class SkillBridgeApplication {

    /**
     * Método de entrada da aplicação Java.
     *
     * @param args argumentos de linha de comando (normalmente não utilizados
     *             em aplicações Spring Boot).
     *
     * Exemplo de execução:
     * - Via IDE (IntelliJ/Eclipse): botão "Run" na classe.
     * - Via linha de comando (jar empacotado):
     *      java -jar skillbridge-api.jar
     */
    public static void main(String[] args) {
        SpringApplication.run(SkillBridgeApplication.class, args);
    }

}
