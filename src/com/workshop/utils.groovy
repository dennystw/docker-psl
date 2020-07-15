#!/usr/bin/groovy
package com.workshop

def checkValidation(LinkedHashMap m){
    def overall_validation = true
    m.each {
        if (!it['value']['status']) {
            println "${it['value']['error_message']}"
            overall_validation = overall_validation && it['value']['status']
        }
    }
    return overall_validation
}

return this