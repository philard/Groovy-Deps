#!/usr/bin/env groovy
package com

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.servlet.ServletContextHandler

@Grab(group='org.eclipse.jetty.aggregate', module='jetty-all', version='7.6.15.v20140411')
def startJetty(publicDir) {
    def server = new Server(8080)

    def handler = new ServletContextHandler(ServletContextHandler.SESSIONS)
    handler.contextPath = '/'
    handler.resourceBase = '.'
    def filesHolder = handler.addServlet(DefaultServlet, '/')
    filesHolder.setInitParameter('resourceBase', publicDir)
 
    server.handler = handler
    server.start()
}
 
println "Starting Jetty, press Ctrl+C to stop."
def publicDir = "./public";
new YextdepToGoJSFiles().execute([".."], publicDir)
startJetty(publicDir);
println "Please open http://localhost:8080/index.html"
