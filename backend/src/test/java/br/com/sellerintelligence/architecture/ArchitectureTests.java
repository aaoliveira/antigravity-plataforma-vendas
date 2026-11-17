package br.com.sellerintelligence.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "br.com.sellerintelligence", importOptions = {ImportOption.DoNotIncludeTests.class})
public class ArchitectureTests {

    @ArchTest
    public static final ArchRule noFieldInjection = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .because("Constructor injection must always be used instead of field injection");

    @ArchTest
    public static final ArchRule controllersMustFollowNamingConvention = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .allowEmptyShould(true)
            .because("Controllers must follow the naming convention Ending with Controller");

    @ArchTest
    public static final ArchRule infrastructureMustNotDependOnTestMocks = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..org.mockito..")
            .because("Production infrastructure classes must never depend on test mocks");
}
