package com.sellerintelligence.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.sellerintelligence", importOptions = ImportOption.DoNotIncludeTests.class)
public class ModularArchitectureTest {

    @ArchTest
    static final ArchRule modules_should_be_free_of_cycles =
            slices().matching("com.sellerintelligence.modules.(*)..")
                    .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule core_should_not_depend_on_domain_modules =
            noClasses().that().resideInAPackage("com.sellerintelligence.core..")
                    .should().dependOnClassesThat().resideInAPackage("com.sellerintelligence.modules..");

    @ArchTest
    static final ArchRule controllers_should_only_reside_in_valid_packages =
            classes().that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAnyPackage("..modules..", "..core..")
                    .allowEmptyShould(true);
}
