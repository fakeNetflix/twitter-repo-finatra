package com.twitter.finatra.http.tests.integration.doeverything.main.modules

import com.google.inject.{Module, Provides}
import com.google.inject.name.{Named, Names}
import com.twitter.conversions.DurationOps._
import com.twitter.finatra.http.tests.integration.doeverything.main.services.{
  ComplexServiceFactory,
  MultiService,
  OneMultiService,
  TwoMultiService
}
import com.twitter.finatra.test.Prod
import com.twitter.inject.{Injector, TwitterModule}

class DoEverythingModule extends TwitterModule {

  // Note: The following flag values are not used in this module, but are @Flag injected elsewhere
  flag("moduleMagicNum", "30", "Module Magic number")
  flag("moduleDuration", 5.seconds, "Module duration")

  /* this is purposely left as a `def` for testing instead of being a val as it would be normally */
  override def modules: Seq[Module] =
    Seq(new DoSomethingElseModule)

  override protected def configure(): Unit = {
    bindSingleton[String](Names.named("str1")).toInstance("string1")
    bind[String](Names.named("str2")).toInstance("string2")

    bindSingleton[Int].toInstance(11)
    bind[String].toInstance("default string")
    bind[Option[String]].toInstance(Some("default option string"))
    bind[String].annotatedWith[Prod].toInstance("prod string")
    bind[Option[String]].annotatedWith[Prod].toInstance(Some("prod option string"))

    bindAssistedFactory[ComplexServiceFactory]()

    val multiBinder = createMultiBinder[MultiService]
    multiBinder.addBinding.to[OneMultiService]
    multiBinder.addBinding.to[TwoMultiService]
  }

  override def singletonStartup(injector: Injector): Unit = {
    assert(injector.instance[String, Prod] == "prod string")
  }

  override def singletonShutdown(injector: Injector): Unit = {
    info("shutdown")
  }

  @Provides
  @Named("example")
  def provideNamedString: String = {
    "named"
  }
}
