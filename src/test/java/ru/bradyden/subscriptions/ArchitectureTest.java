package ru.bradyden.subscriptions;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@AnalyzeClasses(
        packages = "ru.bradyden.subscriptions",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    @ArchTest
    static final ArchRule FEATURE_PACKAGES_ARE_ACYCLIC =
            slices().matching("ru.bradyden.subscriptions.(*)..").should().beFreeOfCycles();

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_ACCESS_REPOSITORIES =
            noClasses()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .dependOnClassesThat()
                    .haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule DOMAIN_AND_APPLICATION_DO_NOT_DEPEND_ON_WEB =
            noClasses()
                    .that()
                    .resideInAPackage("..obligation..")
                    .and()
                    .areNotAnnotatedWith(RestController.class)
                    .and()
                    .areNotAnnotatedWith(RestControllerAdvice.class)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("org.springframework.web..");

    @ArchTest
    static final ArchRule REPOSITORIES_ARE_INTERFACES =
            classes().that().haveSimpleNameEndingWith("Repository").should().beInterfaces();
}
