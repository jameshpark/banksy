package org.jameshpark.banksy.models

import kotlinx.coroutines.*

data class Merchant(
    val name: String,
    val category: Category,
    val regex: String
) {

    private val compiledRegex = regex.toRegex(RegexOption.IGNORE_CASE)

    fun existsIn(description: String): Boolean = compiledRegex.containsMatchIn(description)

}

val merchants = listOf(
    Merchant(name = "Airbnb", category = Category.VACATION, regex = """airbnb"""),
    Merchant(name = "Airport (facilities/parking)", category = Category.CAR_USE_EXPENSE, regex = """airpo?r?t?"""),
    Merchant(name = "Alamo Rental", category = Category.VACATION, regex = """alamo\s?rent"""),
    Merchant(name = "Alaska Airlines", category = Category.VACATION, regex = """alaska\s?air?l?i?n?e?s?"""),
    Merchant(name = "Amazon", category = Category.UNCATEGORIZED, regex = """ama?zo?n(?=\s?ma?r?ke?t?pl?a?c?e?|\s?tips|\.com)"""),
    Merchant(name = "Amazon Prime Membership", category = Category.PRODUCTIVITY, regex = """ama?zo?n\spri?me?"""),
    Merchant(name = "Amazon Web Services", category = Category.HOBBIES, regex = """ama?zo?n\sweb\sse?r?vi?ce?s?"""),
    Merchant(name = "American Airlines", category = Category.VACATION, regex = """american\s?air?l?i?n?e?s?"""),
    Merchant(name = "Apple Store", category = Category.PRODUCTIVITY, regex = """apple(?=\s*store|\s*online|\.com/us)"""),
    Merchant(name = "AT&T", category = Category.PHONE, regex = """at&?t(?=\s*mobility|\*bill)"""),
    Merchant(name = "Audible", category = Category.HOBBIES, regex = """audible(?=,\sinc|\.com)"""),
    Merchant(name = "Autozone", category = Category.CAR_UPKEEP, regex = """autozone"""),
    Merchant(name = "Contains 'barbeque'", category = Category.RESTAURANTS, regex = """barbeque"""),
    Merchant(name = "Contains 'bbq'", category = Category.RESTAURANTS, regex = """bbq"""),
    Merchant(name = "Contains 'beerworks'", category = Category.RESTAURANTS, regex = """beerworks"""),
    Merchant(name = "Contains 'beer'", category = Category.RESTAURANTS, regex = """beer"""),
    Merchant(name = "Best Buy", category = Category.PRODUCTIVITY, regex = """best\s?buy"""),
    Merchant(name = "Contains 'bistro'", category = Category.RESTAURANTS, regex = """bistro"""),
    Merchant(name = "BP Gas", category = Category.GAS, regex = """bp#"""),
    Merchant(name = "Contains 'bread'", category = Category.RESTAURANTS, regex = """bread"""),
    Merchant(name = "Contains 'brewing'", category = Category.RESTAURANTS, regex = """brewing"""),
    Merchant(name = "build.com", category = Category.HOME_IMPROVEMENT, regex = """build\.com"""),
    Merchant(name = "Contains 'burger'", category = Category.RESTAURANTS, regex = """burger"""),
    Merchant(name = "Contains 'cafe'", category = Category.RESTAURANTS, regex = """cafe"""),
    Merchant(name = "Contains 'car wash'", category = Category.CAR_UPKEEP, regex = """car\s*wash"""),
    Merchant(name = "Cava", category = Category.RESTAURANTS, regex = """cava"""),
    Merchant(name = "Chick-Fil-A", category = Category.RESTAURANTS, regex = """chick[\s\-]?fil[\s\-]?a"""),
    Merchant(name = "Contains 'chicken'", category = Category.RESTAURANTS, regex = """chicken"""),
    Merchant(name = "Contains 'china'", category = Category.RESTAURANTS, regex = """china"""),
    Merchant(name = "Chipotle", category = Category.RESTAURANTS, regex = """chipotle"""),
    Merchant(name = "Contains 'cider'", category = Category.RESTAURANTS, regex = """cider"""),
    Merchant(name = "Contains 'coffee'", category = Category.COFFEE, regex = """coffee"""),
    Merchant(name = "Contains 'concessions'", category = Category.RESTAURANTS, regex = """concessions"""),
    Merchant(name = "The Container Store", category = Category.HOME_IMPROVEMENT, regex = """container\s*store"""),
    Merchant(name = "Contains 'cookie'", category = Category.RESTAURANTS, regex = """cookie"""),
    Merchant(name = "Costco", category = Category.GROCERIES, regex = """costco"""),
    Merchant(name = "Crate and Barrel", category = Category.HOME_IMPROVEMENT, regex = """crate\s*(and|&amp;)\s*barrel"""),
    Merchant(name = "Contains 'cuisine'", category = Category.RESTAURANTS, regex = """cuisine"""),
    Merchant(name = "Delta Airlines", category = Category.VACATION, regex = """delta\s?air?l?i?n?e?s?"""),
    Merchant(name = "Contains 'dentist'", category = Category.DENTAL, regex = """dentist"""),
    Merchant(name = "Contains 'dessert'", category = Category.RESTAURANTS, regex = """dessert"""),
    Merchant(name = "Contains 'diner'", category = Category.RESTAURANTS, regex = """diner"""),
    Merchant(name = "Contains 'dining'", category = Category.RESTAURANTS, regex = """dining"""),
    Merchant(name = "Domino's", category = Category.RESTAURANTS, regex = """domino'?s"""),
    Merchant(name = "Contains 'donut'", category = Category.RESTAURANTS, regex = """donut"""),
    Merchant(name = "DoorDash", category = Category.RESTAURANTS, regex = """doordash"""),
    Merchant(name = "Exxon", category = Category.GAS, regex = """exxon"""),
    Merchant(name = "FedEx", category = Category.PRODUCTIVITY, regex = """fedex"""),
    Merchant(name = "Contains 'food hall'", category = Category.RESTAURANTS, regex = """food\s*ha?l?l?"""),
    Merchant(name = "Contains 'food truck'", category = Category.RESTAURANTS, regex = """food\s*tru?c?k?"""),
    Merchant(name = "Contains 'gelato'", category = Category.RESTAURANTS, regex = """gelato"""),
    Merchant(name = "Google Fiber", category = Category.UTILITIES, regex = """google(?=\s\*fiber|\s\*services)"""),
    Merchant(name = "Contains 'grill'", category = Category.RESTAURANTS, regex = """grille?"""),
    Merchant(name = "GrubHub", category = Category.RESTAURANTS, regex = """grubhub"""),
    Merchant(name = "Contains 'gyro'", category = Category.RESTAURANTS, regex = """gyro"""),
    Merchant(name = "Contains 'halal'", category = Category.RESTAURANTS, regex = """halal"""),
    Merchant(name = "Hilton", category = Category.VACATION, regex = """hilton"""),
    Merchant(name = "The Home Depot", category = Category.HOME_UPKEEP, regex = """home\s*depot"""),
    Merchant(name = "Home Goods", category = Category.HOME_IMPROVEMENT, regex = """home\s*goods"""),
    Merchant(name = "Contains 'ice cream'", category = Category.RESTAURANTS, regex = """ice\s*cream"""),
    Merchant(name = "IKEA", category = Category.HOME_IMPROVEMENT, regex = """ikea"""),
    Merchant(name = "Instacart", category = Category.GROCERIES, regex = """instacart"""),
    Merchant(name = "Contains 'japanese'", category = Category.RESTAURANTS, regex = """japane?s?e?"""),
    Merchant(name = "Jeni's Ice Cream", category = Category.RESTAURANTS, regex = """jeni'?s"""),
    Merchant(name = "Jimmy John's", category = Category.RESTAURANTS, regex = """jimmy\sjohns"""),
    Merchant(name = "Contains 'kitchen'", category = Category.RESTAURANTS, regex = """kitchen"""),
    Merchant(name = "LinkedIn", category = Category.PRODUCTIVITY, regex = """linkedin"""),
    Merchant(name = "Lowe's", category = Category.HOME_UPKEEP, regex = """lowe'?s"""),
    Merchant(name = "Lyft", category = Category.TRANSPORTATION, regex = """lyft"""),
    Merchant(name = "McDonald's", category = Category.RESTAURANTS, regex = """mcdonald'?s"""),
    Merchant(name = "Contains 'mexican'", category = Category.RESTAURANTS, regex = """mexic?a?n?"""),
    Merchant(name = "Contains 'music'", category = Category.ENTERTAINMENT, regex = """music"""),
    Merchant(name = "National Car Rental", category = Category.VACATION, regex = """national\s*car"""),
    Merchant(name = "New York Times", category = Category.PRODUCTIVITY, regex = """ne?w?\s*yo?r?k?\s*times"""),
    Merchant(name = "New Balance", category = Category.CLOTHING, regex = """new\sbalance"""),
    Merchant(name = "Nike", category = Category.CLOTHING, regex = """nike"""),
    Merchant(name = "Contains 'noodle'", category = Category.RESTAURANTS, regex = """noodl?e?s?"""),
    Merchant(name = "Nordstrom", category = Category.CLOTHING, regex = """nordstrom"""),
    Merchant(name = "Office Depot", category = Category.PRODUCTIVITY, regex = """office\s*depot"""),
    Merchant(name = "Contains 'parking'", category = Category.TRANSPORTATION, regex = """parking"""),
    Merchant(name = "Party City", category = Category.HOBBIES, regex = """party\s*city"""),
    Merchant(name = "Peacock", category = Category.UTILITIES, regex = """peacock"""),
    Merchant(name = "Petco", category = Category.PET, regex = """petco"""),
    Merchant(name = "Petsmart", category = Category.PET, regex = """petsmart"""),
    Merchant(name = "Contains 'pharmacy'", category = Category.MEDICAL, regex = """pharma?c?y?"""),
    Merchant(name = "Philips", category = Category.ESSENTIALS, regex = """philips"""),
    Merchant(name = "Contains 'pizza'", category = Category.RESTAURANTS, regex = """pizza"""),
    Merchant(name = "Popeye's", category = Category.RESTAURANTS, regex = """popeye'?s"""),
    Merchant(name = "Amazon Prime Video", category = Category.ENTERTAINMENT, regex = """prime\svideo"""),
    Merchant(name = "Contains 'ramen'", category = Category.RESTAURANTS, regex = """ramen"""),
    Merchant(name = "Contains 'restaurant'", category = Category.RESTAURANTS, regex = """restaur?a?n?t?"""),
    Merchant(name = "Contains 'roaster'", category = Category.COFFEE, regex = """roaster"""),
    Merchant(name = "Saks", category = Category.ESSENTIALS, regex = """saks"""),
    Merchant(name = "Contains 'sandwich'", category = Category.RESTAURANTS, regex = """sandwich"""),
    Merchant(name = "Shell", category = Category.GAS, regex = """shell"""),
    Merchant(name = "Sonic Drive In", category = Category.RESTAURANTS, regex = """sonic\s*drive\s*in"""),
    Merchant(name = "Contains 'souvenir'", category = Category.VACATION, regex = """souvenir"""),
    Merchant(name = "Spectrum", category = Category.UTILITIES, regex = """spectrum"""),
    Merchant(name = "St. Regis", category = Category.VACATION, regex = """st\s*\.?\s*regis"""),
    Merchant(name = "Sunoco", category = Category.GAS, regex = """sunoco"""),
    Merchant(name = "Contains 'sushi'", category = Category.RESTAURANTS, regex = """sushi"""),
    Merchant(name = "Contains 'taco'", category = Category.RESTAURANTS, regex = """taco"""),
    Merchant(name = "Contains 'taqueria'", category = Category.RESTAURANTS, regex = """taqueria"""),
    Merchant(name = "Target", category = Category.GROCERIES, regex = """target"""),
    Merchant(name = "Contains 'taste'", category = Category.RESTAURANTS, regex = """taste"""),
    Merchant(name = "Contains 'thai'", category = Category.RESTAURANTS, regex = """thai"""),
    Merchant(name = "Trader Joe's", category = Category.GROCERIES, regex = """trader\sjoe'?s"""),
    Merchant(name = "Contains 'travel'", category = Category.VACATION, regex = """travel"""),
    Merchant(name = "Uber Trip", category = Category.TRANSPORTATION, regex = """uber\s*trip"""),
    Merchant(name = "Uber Eats", category = Category.RESTAURANTS, regex = """uber\seats"""),
    Merchant(name = "Uber One", category = Category.RESTAURANTS, regex = """uber\sone"""),
    Merchant(name = "Uber", category = Category.TRANSPORTATION, regex = """uber"""),
    Merchant(name = "UPS Store", category = Category.PRODUCTIVITY, regex = """ups\s*store"""),
    Merchant(name = "USPS", category = Category.PRODUCTIVITY, regex = """usps"""),
    Merchant(name = "Venmo", category = Category.UNCATEGORIZED, regex = """venmo"""),
    Merchant(name = "Contains 'vintage'", category = Category.HOME_IMPROVEMENT, regex = """vintage"""),
    Merchant(name = "Contains 'vitamin'", category = Category.GROCERIES, regex = """vitamin"""),
    Merchant(name = "Contains 'waffle'", category = Category.RESTAURANTS, regex = """waffle"""),
    Merchant(name = "Wal-Mart", category = Category.GROCERIES, regex = """wal-mart"""),
    Merchant(name = "Walgreens", category = Category.MEDICAL, regex = """walgreens"""),
    Merchant(name = "Wawa", category = Category.GROCERIES, regex = """wawa"""),
    Merchant(name = "Wegman's", category = Category.GROCERIES, regex = """wegman'?s"""),
    Merchant(name = "Wendy's", category = Category.RESTAURANTS, regex = """wendy'?s"""),
    Merchant(name = "West Elm", category = Category.HOME_IMPROVEMENT, regex = """west\selm"""),
    Merchant(name = "Whole Foods", category = Category.GROCERIES, regex = """whole(\s*foods|\s*fds)"""),
    Merchant(name = "Contains 'wine'", category = Category.RESTAURANTS, regex = """wine"""),
    Merchant(name = "YouTube Premium", category = Category.ENTERTAINMENT, regex = """youtubepremium"""),
)

suspend fun categoryFrom(description: String) = coroutineScope {
    merchants.map {
        async {
            if (it.existsIn(description)) {
                it.category
            } else {
                null
            }
        }
    }
        .awaitAll()
        .filterNotNull()
        .firstOrNull() ?: Category.UNCATEGORIZED
}
