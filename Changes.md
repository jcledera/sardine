# Changes #

## 314 ##
  * Option to select depth for PROPFIND requests (#113)
  * Irrelevant circular redirect exception thrown (#112)
  * Add support to retrieve properties as QName including the namespace (#91)
  * Updated HTTP components dependency

## 304 ##
  * Always set Content-Type header
  * Fix multistatus response handler for lighttpd
  * Update HTTP client dependency to 4.1.2 fixing GZIP compression
  * Fix parsing of properties in default namespace when there are additional PROPSTAT elements with thirdparty namespace in front
  * 106. Using thread local for simple date formatter (#106)

## 284 ##
  * Basic lock handling support
  * Make PROPPATCH return multistatus response
  * Allow to define custom namespace for updated properties
  * Retry PUT operations on expectation failure for repeatable entities
  * Ensure the SAX parser does not resolve entities for referenced DTDs in the response

## 253 ##
  * Updated dependency to httpclient-4.1
  * Added log4j dependency
  * GZIP compression support
  * Support for preemptive authentication for Basic scheme
  * Option to configure and override HTTP client instance (#71)
  * Support Expect: Continue for uploads
  * NTLM authentication scheme support
  * Option to add custom headers to GET and PUT requests
  * Redirect handling for PROPFIND requests (#74)
  * Proxy configuration with proxy selector
  * Checked SardineException extending HttpResponseException (#75)
  * Handle URI encoding in href of resource (#72, #78)
  * Determine if resource is directory (#76)