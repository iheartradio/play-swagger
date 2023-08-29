package com.iheart.playSwagger.exception

class MissingBaseSpecException(baseSpecFileName: String)
    extends Exception(s"Missing a $baseSpecFileName.yml or $baseSpecFileName.json to provide base swagger spec")
