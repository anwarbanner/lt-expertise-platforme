package ma.ltexpertise.importation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * Exécuteur des lots d'import : deux lots traités en parallèle au plus, les suivants attendent. L'utilisateur
 * connecté est transmis au fil d'exécution, pour que les colonnes cree_par / modifie_par restent exactes.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class ConfigurationImportAsynchrone {

    public static final String EXECUTEUR_IMPORTS = "executeurImports";

    @Bean(EXECUTEUR_IMPORTS)
    AsyncTaskExecutor executeurImports() {
        ThreadPoolTaskExecutor executeur = new ThreadPoolTaskExecutor();
        executeur.setCorePoolSize(2);
        executeur.setMaxPoolSize(2);
        executeur.setQueueCapacity(100);
        executeur.setThreadNamePrefix("import-lot-");
        executeur.setWaitForTasksToCompleteOnShutdown(true);
        executeur.setAwaitTerminationSeconds(120);
        executeur.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executeur);
    }
}
