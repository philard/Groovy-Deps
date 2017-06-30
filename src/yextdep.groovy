#! /usr/bin/env groovy
package src

import groovy.io.FileType
import groovy.util.XmlSlurper
import groovy.json.JsonOutput

def path = args ? args[0].toString() : '.'
def dir = new File(path)
println "Scanning directory [${dir}] for Hybris extension definition files..."

def extDefFileNamePattern = /extensioninfo.xml/
def parser = new XmlSlurper()
def depsMap = [:]
def inDegreeMap = [:].withDefault { 0 }
def candidates = new LinkedList()
dir.traverse(type: FileType.FILES, nameFilter: extDefFileNamePattern) {
	def extensioninfo = parser.parse(it)
	def extName = extensioninfo.extension.'@name'.toString()
	def deps = extensioninfo.extension.'requires-extension'.collect { dep -> dep.'@name'.toString() }
	println "Found comextension [${extName}] with dependencies [${deps}]."
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

def result = []
while(candidates) {	
	candidates.sort { ext1, ext2 -> inDegreeMap[ext2] - inDegreeMap[ext1] }
	
	def candidate = candidates.removeFirst()

	result << candidate
	depsMap.each { extName, deps ->
		if (result.contains(extName)) return
		if (!deps.contains(candidate)) return

		deps.remove(candidate);
		inDegreeMap[extName] = inDegreeMap[extName] - 1;
		if(!deps) candidates << extName
	}
}

println "****** Suggested Topological Sequence ******"
println result
