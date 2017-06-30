#! /usr/bin/env groovy
package com

import groovy.io.FileType
import groovy.json.JsonOutput

class YextdepToGoJSFiles {
	def execute (args, publicDir = "./public") {

		def path = args ? args[0].toString() : '.'
		def dir = new File(path)
		println "Scanning directory [${dir.getCanonicalPath()}] for Hybris extension definition files..."

		def extDefFileNamePattern = /extensioninfo.xml/
		def parser = new XmlSlurper()
		def depsMap = [:]
		def inDegreeMap = [:].withDefault { 0 }
		def candidates = new LinkedList()
		dir.traverse(type: FileType.FILES, nameFilter: extDefFileNamePattern) {
			def extensioninfo = parser.parse(it)
			def extName = extensioninfo.extension.'@name'.toString()
			def deps = extensioninfo.extension.'requires-extension'.collect { dep -> dep.'@name'.toString() }
			println "Found extension [${extName}] with dependencies [${deps}]."
			depsMap[extName] = deps
		}


		def extNames = depsMap.keySet()
		extNames.each { extName ->
			def deps = depsMap[extName].findAll { extNames.contains(it) }
			depsMap[extName] = deps

			if(deps) {
				deps.each { inDegreeMap[it] = inDegreeMap[it] + 1 }
			}
			else {
				candidates << extName
			}
		}

		println "****** Dependency Info ******"
		println JsonOutput.prettyPrint(JsonOutput.toJson(depsMap))
    
    writeGoJSFiles(depsMap, inDegreeMap, extNames, publicDir)
    
		def result = []
		while(candidates) {
			candidates.sort { ext1, ext2 -> inDegreeMap[ext2] - inDegreeMap[ext1] }

			def candidate = candidates.removeFirst()

			result << candidate
			depsMap.each { extName, deps ->
				if (result.contains(extName)) return
				if (!deps.contains(candidate)) return

				deps.remove(candidate)
				inDegreeMap[extName] = inDegreeMap[extName] - 1
				if(!deps) candidates << extName
			}
		}

		println "****** Suggested Topological Sequence ******"
		println result    
    writeResultFile(result, publicDir, dir)
    
	}
  
  def writeResultFile (result, publicDir, dir) {
    result = "Scanned directory [${dir.getCanonicalPath()}] for Hybris extension definition files..." + 
    "<h2>****** Suggested Topological Sequence ******</h2>" + 
    "<div>" + result + "</div>" 
    def resultFile = new File(publicDir + "/data/result.txt")
		resultFile.write result
  }
  
  def writeGoJSFiles (depsMap, inDegreeMap, extNames, publicDir) {

    def nodes = new ArrayList()
    def links = new ArrayList()
		
    depsMap.each{ extName, deps -> 
      links.addAll(extNamesToLink(extName, deps))
		}
    
    def maxDegree = 0
    inDegreeMap.each { extName, degree ->
      if (degree > maxDegree) maxDegree = degree
    }
    extNames.each { extName -> 
      nodes.add(extNameToNode(extName, inDegreeMap[extName], maxDegree))
    }
    
		def nodeData = JsonOutput.prettyPrint(JsonOutput.toJson(nodes))
		def nodesFile = new File(publicDir + "/data/nodeData.json")
		nodesFile.write nodeData

		def linksData = JsonOutput.prettyPrint(JsonOutput.toJson(links))
		def linksFile = new File(publicDir + "/data/linkData.json")
		linksFile.write linksData
  }

	def extNameToNode(extName, degree, maxDegree) {
    def maxColor = 0xff * 3
    def colorMagnitude = (maxColor * degree).intdiv(maxDegree) //maximum value is 0xee
    def redGreenBlue = ""
    (1..3).each { //Spread colorMagnitude over red, then green, then blue.
      int thisColor
      if (colorMagnitude > 0xff) {
        thisColor = 0xff
      } else {
        thisColor = colorMagnitude
      }
      redGreenBlue += String.format("%02X", thisColor) //eg 255 -> "FF"
      
      colorMagnitude = Math.max(0, colorMagnitude - 0xff)
    }
    
    return ["key":extName, "text":extName, "color":"#"+redGreenBlue]
	}

	def extNamesToLink(fromName, toNames) {
		def links = new ArrayList()
		toNames.each { toName ->
			links.add(["from":fromName, "to":toName, "color":"#123456"])
		}
		return links
	}

}
