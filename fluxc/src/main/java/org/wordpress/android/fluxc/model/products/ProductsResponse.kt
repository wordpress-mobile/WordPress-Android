package org.wordpress.android.fluxc.model.products

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@JsonAdapter(ProductsDeserializer::class)
class ProductsResponse(val products: List<Product>)

class ProductsDeserializer : JsonDeserializer<ProductsResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ProductsResponse {
        val productType: Type = object : TypeToken<HashMap<String?, Product?>?>() {}.type
        val productsMap: HashMap<String, Product> = Gson().fromJson(json, productType)
        return ProductsResponse(productsMap.values.toList())
    }
}
