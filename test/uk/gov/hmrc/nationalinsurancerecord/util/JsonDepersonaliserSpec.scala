/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nationalinsurancerecord.util

import play.api.libs.json._

class JsonDepersonaliserSpec extends UnitSpec {

  "depersonaliseNumber" when {
    "when number is 1234567890" must {
      "return 1111111111" in {
        val TestNumber = 1234567890
        val Expected = 1111111111
        JsonDepersonaliser.depersonaliseNumber(BigDecimal.apply(TestNumber)) shouldBe BigDecimal.apply(Expected)
      }
    }
  }

  "depersonaliseString" when {
    "when string contains all the upper case letters" must {
      "return a string of a's" in {
        JsonDepersonaliser.depersonaliseString("ABCDEFGHIJKLMNOPQRSTUVWXYZ") shouldBe "aaaaaaaaaaaaaaaaaaaaaaaaaa"
      }
    }

    "when string contains all the lower case letters" must {
      "return a string of a's" in {
        JsonDepersonaliser.depersonaliseString("abcdefghijklmnopqrstuvwxyz") shouldBe "aaaaaaaaaaaaaaaaaaaaaaaaaa"
      }
    }

    "when string contains the numbers 0-9" must {
      "returns all 1's" in {
        JsonDepersonaliser.depersonaliseString("1234567890") shouldBe "1111111111"
      }
    }

    "when string contains punctuation" must {
      "return the same punctuation" in {
        JsonDepersonaliser.depersonaliseString(": {}[],'\"") shouldBe ": {}[],'\""
      }
    }
  }

  "depersonaliseValue" when {
    "when value is an array" must {
      "depersonalise all eleemnts" in {
        val TestNumber = 99
        val DepersonalisedNumber = 11
        val array = JsArray(Seq(JsNumber(TestNumber), JsString("xyz")))
        val expected = JsArray(Seq(JsNumber(DepersonalisedNumber), JsString("aaa")))
        JsonDepersonaliser.depersonaliseValue(array) shouldBe expected
      }
    }

    "when value is a boolean true" must {
      "return false" in {
        JsonDepersonaliser.depersonaliseValue(JsBoolean(true)) shouldBe JsBoolean(false)
      }
    }

    "when value is a boolean false" must {
      "return false" in {
        JsonDepersonaliser.depersonaliseValue(JsBoolean(false)) shouldBe JsBoolean(false)
      }
    }

    "when value is a number" must {
      "depersonalise the number" in {
        val TestNumber = 99
        val DepersonalisedNumber = 11
        JsonDepersonaliser.depersonaliseValue(JsNumber(TestNumber)) shouldBe JsNumber(DepersonalisedNumber)
      }
    }

    "when value is an object" must {
      "should depersonalise all attributes" in {
        val obj = JsObject(Map("firstName" -> JsString("John"), "lastName" -> JsString("Doe")))
        val expected = JsObject(Map("firstName" -> JsString("aaaa"), "lastName" -> JsString("aaa")))
        JsonDepersonaliser.depersonaliseValue(obj) shouldBe expected
      }
    }

    "when value is a string" must {
      "depersonalise the string" in {
        JsonDepersonaliser.depersonaliseValue(JsString("XYZ")) shouldBe JsString("aaa")
      }
    }

    "when value is null" must {
      "return null" in {
        JsonDepersonaliser.depersonaliseValue(JsNull) shouldBe JsNull
      }
    }
  }

  "depersonaliseArray" when {
    "when passed a zero-length array" must {
      "return a zero-length array" in {
        val array = JsArray(Seq())
        JsonDepersonaliser.depersonaliseArray(array) shouldBe array
      }
    }
  }

  "depersonaliseObject" when {
    "when passed an empty object" must {
      "return an empty object" in {
        val obj = JsObject(Map[String, JsValue]())
        JsonDepersonaliser.depersonaliseObject(obj) shouldBe obj
      }
    }
  }

  "depersonalise" when {
    "when passed an object" must {
      "succeed and return depersonalised JSON" in {
        val obj = JsObject(Map("firstName" -> JsString("John"), "lastName" -> JsString("Doe")))
        val expected = JsObject(Map("firstName" -> JsString("aaaa"), "lastName" -> JsString("aaa")))
        JsonDepersonaliser.depersonalise(obj) shouldBe s"Depersonalised JSON\n${Json.prettyPrint(expected)}"
      }
    }

    "when passed an array" must {
      "succeed and return a depersonalised representation of the array" in {
        val TestNumber = 99
        val DepersonalisedNumber = 11
        val array = JsArray(Seq(JsNumber(TestNumber), JsString("xyz")))
        val expected = JsArray(Seq(JsNumber(DepersonalisedNumber), JsString("aaa")))
        JsonDepersonaliser.depersonalise(array) shouldBe s"Depersonalised JSON\n${Json.prettyPrint(expected)}"
      }
    }

    "when passed a boolean" must {
      "succeed and return a depersonalised representation of the boolean value" in {
        JsonDepersonaliser.depersonalise(JsBoolean(true)) shouldBe s"Depersonalised JSON\nfalse"
      }
    }

    "when passed a number" must {
      "succeed and return a depersonalised representation of the string value" in {
        val TestNumber = 99
        JsonDepersonaliser.depersonalise(JsNumber(TestNumber)) shouldBe s"Depersonalised JSON\n11"
      }
    }

    "when passed a string" must {
      "succeed and return a depersonalised representation of the string value" in {
        JsonDepersonaliser.depersonalise(JsString("XYZ")) shouldBe s"Depersonalised JSON\n\"aaa\""
      }
    }

    "when passed JSON null" must {
      "succeed and return JSON null" in {
        JsonDepersonaliser.depersonalise(JsNull) shouldBe s"Depersonalised JSON\nnull"
      }
    }

    "when passed a null reference" must {
      "return Failure" in {
        // scalastyle:off null
        JsonDepersonaliser.depersonalise(null) shouldBe "JSON could not be depersonalised\nscala.MatchError: null"
        // scalastyle:on null
      }
    }
  }

}
