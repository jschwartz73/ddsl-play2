package com.kjetland.ddsl.play2

import play.api.{Logger, Play, Plugin}
import java.lang.RuntimeException
import com.kjetland.ddsl.{DdslClientCacheReadsImpl, DdslClientImpl, DdslClient}
import com.kjetland.ddsl.config.DdslConfig
import com.kjetland.ddsl.model.{ServiceId, ServiceLocation, Service}
import com.kjetland.ddsl.utils.NetUtils
import org.joda.time.DateTime

class DdslPlugin(val app:play.api.Application) extends Plugin with DdslConfig with PropsUtils{

  var client:DdslClient = null
  var ddslEnvironment:String = null



  override def onStart() {
    ddslEnvironment = getProp("ddsl.environment", "test", false)
    val ttl_mills : Long = getProp("ddsl.client_cache_ttl_mills", "1000", false).toLong
    Logger.info("DDSL loading config from application.conf. using ddsl.environment=" + ddslEnvironment+ ". client read cache: " + ttl_mills + " mills")

    val realClient = new DdslClientImpl( this )
    client = new DdslClientCacheReadsImpl( realClient, ttl_mills)

    getServiceId() match {
      case Some(sid:ServiceId) => {
        val sl = getServiceLocation()
        Logger.info("Registering this service as Up. ServiceID: " + sid + " ServiceLocation: " + sl);
        client.serviceUp( new Service(sid, sl))
      }
      case None => {
        //nothing to register - we're done.
      }
    }

  }

  override def onStop() {
    //must terminate our client to make sure any registered services go offline
    if( client != null ){
      client.disconnect()
      client = null
    }

  }

  // Must resolve our serviceLocation
  private def getServiceLocation(): ServiceLocation = {
    val port = app.configuration.getInt("http.port").getOrElse(9000)
    val ip:String = NetUtils.resolveLocalPublicIP()
    //TODO: does not work when mounted on context other than / - ie as war in tomcat etc..
    val url : String = "http://"+ip+":"+port + "/"
    val quality:Double = getProp("ddsl.locationquality", "0.0", false).toDouble

    return ServiceLocation(url, quality, new DateTime(), null)
  }

  private def getServiceId(): Option [ServiceId] = {
    //are we broadcasting?
    if( !"true".equalsIgnoreCase( getProp("ddsl.broadcastservice", "false", false) )){
      //not broadcasting
      return None
    }
    //we are broadcasting - needs serviceId config
    //ddsl.serviceid.environment=test
    //ddsl.serviceid.type=http
    //ddsl.serviceid.name=PlayExampleServer
    //ddsl.serviceid.version=1.0
    return Some(ServiceId( ddslEnvironment,
      getProp("ddsl.serviceid.type", null, true),
      getProp("ddsl.serviceid.name", null, true),
      getProp("ddsl.serviceid.version", null, true)))
  }

  override def hosts:String = {
    val zkHostList:String = getProp("ddsl.zkhostslist", "localhost:2181", false)
    Logger.info("DDSL (application.conf) using ddsl.zkhostslist=" + zkHostList)
    return zkHostList
  }

  override def getStaticUrl ( sid:ServiceId ):String = {

    // sid.getMapKey returns string on the form 'ServiceId(test,http,Play2ExampleServer,1.0)' which is not a valid key
    // when reading play config proerties, so we have to transform it into:
    // 'ServiceId.test.http.Play2ExampleServer.1.0'
    val key = "ddsl.fallback."+sid.getMapKey.replace('(','.').replace(',','.').replace(")","")

    Logger.info("Cannot find url via ddsl - looking for fallback url in application.conf with key: " + key)
    val url = getProp(key, null, false)
    if( url == null) throw new RuntimeException("Error resolving fallback url from application.conf with key: " + key)

    return url
  }


}


trait PropsUtils {
  def app : play.api.Application

  def getProp( name:String, defaultValue:String, mustHave:Boolean) : String = {
    app.configuration.getString(name).getOrElse( {
      if ( mustHave ) throw new RuntimeException("Needed config property not found in application.conf: " + name)
      return defaultValue
    } )
  }

}