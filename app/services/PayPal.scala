package services

import play.api.libs.ws.WS
import models.{Track, Album}
import utils.Utils.urldecode
import play.api.Play
import com.ning.http.client.AsyncHttpClientConfig
import java.text.DecimalFormat

/**
 * Created by IntelliJ IDEA.
 * User: Keyston
 * Date: 7/25/12
 * Time: 4:48 PM
 */

case class Nvp() {
  var data = Map.empty[String, Seq[String]]

  def put(key: String, value: Any) = {
    data += key -> Seq(value.toString)
    this
  }
}

case class PayPal() {

  import play.api.Play.current

  import java.util.concurrent.TimeUnit

  val decimalFormat = new DecimalFormat("##.00")
  lazy val app = Play.application
  lazy val config = app.configuration

  lazy val password = config.getString("paypal.password").get
  lazy val username = config.getString("paypal.username").get
  lazy val signature = config.getString("paypal.signature").get
  lazy val env = config.getString("paypal.env").get
  lazy val timeout = java.lang.Long.parseLong(config.getString("paypal.timeout").get)

  lazy val api = "https://api-3t.%s.paypal.com/nvp".format(env)

  lazy val site = if (env == "sandbox") "https://www.sandbox.paypal.com/cgi-bin/webscr?" else "https://www.paypal.com/cgi-bin/webscr?"


  private def defaultParams(method: String) = {
    val params = new Nvp()





    // Add login infos
    params.put("METHOD", method);
    params.put("VERSION", "62.0");
    params.put("USER", username);
    params.put("PWD", password);
    params.put("SIGNATURE", signature);
    params
  }

  def express(title: String, amount: Double, returnURL: String, cancelURL: String, currencyCode: String = "USD") = {
    val params = defaultParams("SetExpressCheckout")

    // Add login infos
    params.put("METHOD", "SetExpressCheckout");
    params.put("VERSION", "62.0");
    params.put("USER", username);
    params.put("PWD", password);
    params.put("SIGNATURE", signature);

    //

    params.put("RETURNURL", returnURL);
    params.put("CANCELURL", cancelURL);
    params.put("LOCALECODE", "US");
    params.put("NOSHIPPING", "1"); // No shipping address
    // params.put("SOLUTIONTYPE", "Sole"); // No paypal account needed
    // params.put("LANDINGPAGE", "Billing"); // Non login page
    // params.put("CHANNELTYPE", "Merchant");
    params.put("PAYMENTREQUEST_0_AMT", decimalFormat.format(amount))

    params.put("L_PAYMENTREQUEST_0_NAME0", title)
    params.put("L_PAYMENTREQUEST_0_AMT0", decimalFormat.format(amount))
    params.put("L_PAYMENTREQUEST_0_DESC0", title)
    params.put("PAYMENTREQUEST_0_PAYMENTACTION", "Sale")
    //params.put("PAYMENTACTION", paymentType);
    params.put("CURRENCYCODE", currencyCode);
    params.put("PAYMENTREQUEST_0_CUSTOM", title)



    val results = call(params)
    results.get("TOKEN")
  }

  def details(token: String) = {
    val params = defaultParams("GetExpressCheckoutDetails")
    params.put("TOKEN", token)
    call(params)
  }

  def commit(results: Map[String, String]) = {
    call(defaultParams("DoExpressCheckoutPayment")
      .put("TOKEN", results get "TOKEN" get)
      .put("AMT", results get "AMT" get)
      .put("PAYERID", results get "PAYERID" get)
      .put("PAYMENTACTION", "Sale"))

  }


  def call(params: Nvp, timeout: Long = timeout) = {
    // Call paypal

    val result = WS.url(api).post(params.data)
    val response = result.await(timeout, TimeUnit.SECONDS).get.body

    // Parse result
    response.split("&").map(_.split("=")).map(p => (p(0) -> decode(p.drop(1).mkString("")))).toMap


  }

  private def decode(data: String) = java.net.URLDecoder.decode(data, "UTF-8")
}

object PayPal {

  import play.api.Play.current

  lazy val app = Play.application
  lazy val config = app.configuration
  lazy val env = config.getString("paypal.env").get
  lazy val site = if (env == "sandbox") "https://www.sandbox.paypal.com/cgi-bin/webscr?" else "https://www.paypal.com/cgi-bin/webscr?"
  lazy val service = new PayPal();

  def url(token: String) = site + "cmd=_express-checkout&token=" + token

  def ok(results: Map[String, String]) = results.get("ACK").map(_.equals("Success")).getOrElse(false)

  def commit(results: Map[String, String]) = service.commit(results)

  def details(token: String) = {
    service.details(token)
  }

  def apply(title: String, amount: Double, returnURL: String, cancelURL: String, currencyCode: String = "USD") = {
    service.express(title, amount, returnURL, cancelURL, currencyCode)
  }
}