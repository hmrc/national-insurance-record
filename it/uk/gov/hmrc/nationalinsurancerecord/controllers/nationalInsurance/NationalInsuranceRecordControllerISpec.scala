package uk.gov.hmrc.nationalinsurancerecord.controllers.nationalInsurance

import com.github.tomakehurst.wiremock.client.WireMock._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, status => statusResult, _}
import uk.gov.hmrc.nationalinsurancerecord.controllers.actions.CopeExclusionAction
import uk.gov.hmrc.nationalinsurancerecord.test_utils.{FakeAction, IntegrationBaseSpec}

class NationalInsuranceRecordControllerISpec extends IntegrationBaseSpec with Results {

  val mockCopeExclusionAction = mock[CopeExclusionAction]

  override def fakeApplication() = GuiceApplicationBuilder()
    .overrides(
      bind[CopeExclusionAction].to(mockCopeExclusionAction)
    )
    .configure(
    "microservice.services.auth.port" -> server.port(),
    "microservice.services.des-hod.host" -> "127.0.0.1",
    "microservice.services.des-hod.port" -> server.port(),
    "auditing.enabled" -> false
  ).build()

  override def beforeEach() = {
    super.beforeEach()

    val authResponse =
      s"""
        |{
        | "nino": "$nino",
        | "authProviderId": { "ggCredId": "xyz" }
        |}
        |""".stripMargin

    stubPostServer(ok(authResponse), "/auth/authorise")
    when(mockCopeExclusionAction.filterCopeExclusions(any())).thenReturn(new FakeAction[AnyContent]())
  }

  val nino = generateNino

  "NationalInsuranceRecordController" must {
    val desUrl = s"/individuals/${nino.withoutSuffix}/pensions/ni"
    val controllerUrl = s"/ni/$nino"

    List(
      notFound() -> "404" -> NOT_FOUND,
      badRequest() -> "400" -> BAD_REQUEST,
      gatewayTimeout() -> "504" -> GATEWAY_TIMEOUT,
      badGateway() -> "502" -> BAD_GATEWAY,
      serverError() -> "500" -> BAD_GATEWAY,
      unauthorized() -> "401" -> BAD_GATEWAY
    ).foreach {
      case ((errorResponse, errorCode), statusCode) =>
        s"return status code $statusCode for $errorCode" in {

          stubGetServer(errorResponse, desUrl)

          val request = FakeRequest(GET, controllerUrl).withHeaders("Accept" -> "application/vnd.hmrc.1.0+json")

          val result = route(app, request)

          result.map(statusResult) shouldBe Some(statusCode)
        }
    }
  }
}
