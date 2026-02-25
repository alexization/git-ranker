package com.gitranker.api.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.gitranker.api",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureGuardrailTest {

    @ArchTest
    static final ArchRule domain_should_not_depend_on_batch =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..batch..");

    @ArchTest
    static final ArchRule infrastructure_should_not_depend_on_batch =
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat().resideInAPackage("..batch..");

    @ArchTest
    static final ArchRule global_should_not_depend_on_batch =
            noClasses()
                    .that().resideInAPackage("..global..")
                    .should().dependOnClassesThat().resideInAPackage("..batch..");

    @ArchTest
    static final ArchRule rest_controllers_should_reside_in_domain_package =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..domain..");

    @ArchTest
    static final ArchRule controller_advices_should_reside_in_global_package =
            classes()
                    .that().areAnnotatedWith(RestControllerAdvice.class)
                    .should().resideInAPackage("..global..");
}
