package com.gainforge.app.logic

import com.gainforge.app.data.*
import com.gainforge.app.data.Allergen.*
import java.time.LocalDate
import kotlin.math.roundToInt

enum class MealType(val label: String) { BREAKFAST("Breakfast"), MAIN("Lunch & Dinner"), SNACK("Snack"), SHAKE("Shake") }

data class Recipe(
    val id: String,
    val name: String,
    val emoji: String,
    val type: MealType,
    val cuisine: String,
    val proteins: List<String>,
    val carbs: List<String>,
    val allergens: Set<Allergen>,
    val minutes: Int,
    val kcal: Int,
    val protein: Int,
    val carbsG: Int,
    val fat: Int,
    val ingredients: List<String>,
    val steps: List<String>
)

data class PlannedMeal(val slot: String, val recipe: Recipe, val servings: Double, val reason: String) {
    val kcal get() = (recipe.kcal * servings).roundToInt()
    val protein get() = (recipe.protein * servings).roundToInt()
}

object Recipes {
    private fun r(
        id: String, name: String, emoji: String, type: MealType, cuisine: String,
        proteins: List<String>, carbs: List<String>, allergens: Set<Allergen>, minutes: Int,
        kcal: Int, p: Int, c: Int, f: Int, ingredients: List<String>, steps: List<String>
    ) = Recipe(id, name, emoji, type, cuisine, proteins, carbs, allergens, minutes, kcal, p, c, f, ingredients, steps)

    val all: List<Recipe> = listOf(
        // ---------- Breakfast ----------
        r("b_oats", "Peanut Butter Banana Oats", "🥣", MealType.BREAKFAST, "American", listOf("Whey"), listOf("Oats", "Fruit"), setOf(DAIRY, NUTS), 10, 720, 42, 88, 22,
            listOf("100 g rolled oats", "300 ml whole milk", "1 scoop vanilla whey", "1 banana, sliced", "2 tbsp peanut butter", "Pinch of cinnamon"),
            listOf("Simmer oats in milk for 4–5 minutes, stirring.", "Take off the heat and stir in the whey.", "Top with banana, peanut butter and cinnamon.")),
        r("b_skillet", "Loaded Egg & Potato Skillet", "🍳", MealType.BREAKFAST, "American", listOf("Eggs"), listOf("Potatoes"), setOf(EGGS, DAIRY), 20, 680, 38, 52, 34,
            listOf("4 eggs", "250 g potatoes, diced small", "30 g cheddar", "½ bell pepper", "1 tbsp olive oil", "Salt, pepper, paprika"),
            listOf("Microwave diced potatoes 4 minutes to soften.", "Crisp them in oil with the pepper for 6–8 minutes.", "Push aside, scramble the eggs in the pan, then top with cheese.")),
        r("b_burrito", "Turkey Sausage Breakfast Burritos", "🌯", MealType.BREAKFAST, "Mexican", listOf("Eggs", "Turkey"), listOf("Tortillas"), setOf(EGGS, GLUTEN, DAIRY), 15, 750, 48, 62, 32,
            listOf("2 large flour tortillas", "3 eggs", "100 g turkey sausage", "30 g shredded cheese", "Salsa", "Handful of spinach"),
            listOf("Brown the sausage, then add spinach until wilted.", "Scramble the eggs in the same pan.", "Fill tortillas with eggs, sausage, cheese and salsa; roll and toast seam-side down.")),
        r("b_yogurt", "Greek Yogurt Power Bowl", "🫐", MealType.BREAKFAST, "Mediterranean", listOf("Greek yogurt"), listOf("Oats", "Fruit"), setOf(DAIRY, NUTS, GLUTEN), 5, 620, 40, 72, 18,
            listOf("300 g Greek yogurt (2% or whole)", "60 g granola", "1 cup berries", "1 tbsp maple syrup", "15 g almonds"),
            listOf("Spoon yogurt into a bowl.", "Top with granola, berries, almonds and syrup.")),
        r("b_pancakes", "Protein Pancakes", "🥞", MealType.BREAKFAST, "American", listOf("Eggs", "Whey"), listOf("Oats", "Fruit"), setOf(EGGS, DAIRY), 15, 640, 46, 74, 16,
            listOf("80 g oats, blended into flour", "1 scoop whey", "2 eggs", "1 banana", "100 ml milk", "1 tsp baking powder", "Berries and maple syrup to serve"),
            listOf("Blend everything except toppings into a batter.", "Cook ¼-cup pancakes on a medium pan, 2 minutes each side.", "Stack and top with berries and syrup.")),
        r("b_tofu", "Tofu Scramble & Avocado Toast", "🥑", MealType.BREAKFAST, "American", listOf("Tofu"), listOf("Bread"), setOf(SOY, GLUTEN), 15, 610, 34, 58, 26,
            listOf("250 g firm tofu", "2 slices whole-grain bread", "½ avocado", "¼ tsp turmeric", "Spinach", "Salt, pepper, garlic powder"),
            listOf("Crumble tofu into a hot oiled pan with turmeric and garlic powder.", "Cook 5 minutes, then fold in spinach.", "Toast bread, smash avocado on top, and serve with the scramble.")),
        r("b_frenchtoast", "Cinnamon French Toast", "🍞", MealType.BREAKFAST, "American", listOf("Eggs"), listOf("Bread", "Fruit"), setOf(EGGS, DAIRY, GLUTEN), 15, 700, 34, 86, 22,
            listOf("4 thick slices brioche or Texas toast", "3 eggs", "100 ml milk", "1 tsp cinnamon", "Butter", "Strawberries and syrup"),
            listOf("Whisk eggs, milk and cinnamon.", "Dip bread and fry in butter until golden, 2–3 minutes a side.", "Top with strawberries and syrup.")),
        r("b_shakshuka", "Shakshuka with Pita", "🍅", MealType.BREAKFAST, "Mediterranean", listOf("Eggs"), listOf("Bread"), setOf(EGGS, GLUTEN, DAIRY), 25, 590, 30, 56, 26,
            listOf("4 eggs", "1 can crushed tomatoes", "½ onion, 1 pepper", "1 tsp cumin, 1 tsp paprika", "30 g feta", "2 pita breads"),
            listOf("Soften onion and pepper in oil, add spices and tomatoes, simmer 8 minutes.", "Make 4 wells and crack in the eggs; cover and cook 5–6 minutes.", "Top with feta and serve with warm pita.")),

        // ---------- Lunch & Dinner ----------
        r("m_teriyaki", "Teriyaki Chicken Rice Bowl", "🍗", MealType.MAIN, "Asian", listOf("Chicken"), listOf("Rice"), setOf(SOY, GLUTEN), 25, 780, 55, 98, 14,
            listOf("200 g chicken thigh, cubed", "1½ cups cooked rice", "3 tbsp teriyaki sauce", "1 cup broccoli", "Sesame seeds, green onion"),
            listOf("Sear chicken in a hot pan, 6–7 minutes.", "Add broccoli and a splash of water; cover 3 minutes.", "Toss with teriyaki and serve over rice with sesame and green onion.")),
        r("m_bolognese", "Beef Bolognese", "🍝", MealType.MAIN, "Italian", listOf("Beef"), listOf("Pasta"), setOf(GLUTEN, DAIRY), 30, 850, 52, 92, 28,
            listOf("150 g lean ground beef", "120 g dry pasta", "1 cup marinara", "½ onion, 2 garlic cloves", "20 g parmesan"),
            listOf("Boil pasta.", "Brown beef with onion and garlic, add marinara and simmer 10 minutes.", "Toss with pasta and top with parmesan.")),
        r("m_salmon", "Garlic Salmon & Roast Potatoes", "🐟", MealType.MAIN, "Mediterranean", listOf("Fish"), listOf("Potatoes"), setOf(FISH), 25, 760, 46, 62, 34,
            listOf("180 g salmon fillet", "300 g baby potatoes, halved", "2 garlic cloves", "1 tbsp olive oil", "Lemon, dill, green beans"),
            listOf("Roast potatoes at 220 °C / 425 °F for 15 minutes.", "Add salmon and green beans with garlic and oil; roast 10–12 minutes more.", "Finish with lemon and dill.")),
        r("m_tacos", "Steak Tacos", "🌮", MealType.MAIN, "Mexican", listOf("Beef"), listOf("Tortillas"), setOf(DAIRY), 25, 790, 50, 70, 32,
            listOf("200 g flank or skirt steak", "4 corn tortillas", "Onion, cilantro, lime", "30 g cotija or feta", "Salsa verde"),
            listOf("Season steak with salt, cumin and chili; sear 3–4 minutes a side, rest 5 minutes.", "Slice thin against the grain.", "Load warm tortillas with steak, onion, cilantro, cheese and salsa.")),
        r("m_butterchicken", "Butter Chicken & Basmati", "🍛", MealType.MAIN, "Indian", listOf("Chicken"), listOf("Rice"), setOf(DAIRY), 35, 880, 54, 96, 28,
            listOf("200 g chicken breast", "1½ cups cooked basmati", "½ cup tomato passata", "3 tbsp cream", "1 tbsp butter", "2 tsp garam masala, ginger, garlic"),
            listOf("Brown chicken in butter with ginger and garlic.", "Add passata and spices, simmer 12 minutes.", "Stir in cream and serve over basmati.")),
        r("m_chickpea", "Chickpea Coconut Curry", "🥥", MealType.MAIN, "Indian", listOf("Beans & lentils"), listOf("Rice"), emptySet(), 25, 760, 26, 110, 24,
            listOf("1 can chickpeas", "½ can light coconut milk", "½ can chopped tomatoes", "Onion, garlic, ginger", "2 tsp curry powder", "1½ cups cooked rice", "Spinach"),
            listOf("Soften onion, garlic and ginger; add curry powder for 30 seconds.", "Add chickpeas, tomatoes and coconut milk; simmer 12 minutes.", "Wilt in spinach and serve over rice.")),
        r("m_chili", "Turkey & Bean Chili Bowl", "🌶️", MealType.MAIN, "American", listOf("Turkey", "Beans & lentils"), listOf("Rice"), emptySet(), 35, 720, 56, 78, 18,
            listOf("200 g ground turkey", "½ can kidney beans", "½ can tomatoes", "Onion, pepper", "Chili powder, cumin", "1 cup cooked rice"),
            listOf("Brown turkey with onion and pepper.", "Add beans, tomatoes and spices; simmer 20 minutes.", "Serve over rice. Makes great leftovers.")),
        r("m_burger", "Smash Burger & Sweet Potato Fries", "🍔", MealType.MAIN, "American", listOf("Beef"), listOf("Potatoes", "Bread"), setOf(GLUTEN, DAIRY), 25, 950, 54, 80, 44,
            listOf("180 g ground beef (two balls)", "Brioche bun", "1 slice cheese", "250 g sweet potato fries", "Lettuce, pickles, sauce"),
            listOf("Bake or air-fry the fries.", "Smash beef balls flat on a very hot pan, 2 minutes, flip, add cheese.", "Stack on a toasted bun with toppings.")),
        r("m_shrimppasta", "Garlic Shrimp Pasta", "🍤", MealType.MAIN, "Italian", listOf("Shellfish"), listOf("Pasta"), setOf(SHELLFISH, GLUTEN, DAIRY), 20, 780, 44, 90, 24,
            listOf("180 g shrimp", "120 g linguine", "3 garlic cloves", "1 tbsp butter + 1 tbsp olive oil", "Lemon, parsley, chili flakes", "20 g parmesan"),
            listOf("Boil linguine.", "Sauté garlic in butter and oil, add shrimp for 3 minutes.", "Toss with pasta, lemon, a splash of pasta water and parmesan.")),
        r("m_friedrice", "Pork Fried Rice", "🍚", MealType.MAIN, "Asian", listOf("Pork", "Eggs"), listOf("Rice"), setOf(SOY, EGGS, GLUTEN), 20, 820, 42, 96, 28,
            listOf("150 g pork tenderloin, diced", "2 cups day-old rice", "2 eggs", "1 cup peas & carrots", "2 tbsp soy sauce", "Green onion"),
            listOf("Stir-fry pork until cooked; set aside.", "Scramble eggs, add veg and rice, fry until hot.", "Return pork, add soy sauce and green onion.")),
        r("m_tofunoodle", "Crispy Tofu Peanut Noodles", "🥢", MealType.MAIN, "Asian", listOf("Tofu"), listOf("Noodles"), setOf(SOY, GLUTEN, NUTS), 25, 760, 34, 92, 28,
            listOf("200 g firm tofu, cubed", "100 g noodles", "2 tbsp peanut butter + 1 tbsp soy sauce + lime", "Shredded carrot, cucumber", "Cornstarch"),
            listOf("Toss tofu in cornstarch and pan-fry until crispy.", "Boil noodles; whisk peanut butter, soy, lime and hot water into a sauce.", "Toss noodles in sauce, top with tofu and veg.")),
        r("m_gyro", "Chicken Gyro Bowl", "🥙", MealType.MAIN, "Mediterranean", listOf("Chicken", "Greek yogurt"), listOf("Rice", "Bread"), setOf(DAIRY, GLUTEN), 25, 800, 58, 82, 24,
            listOf("200 g chicken thigh", "1 cup cooked rice", "1 pita", "Tzatziki (Greek yogurt, cucumber, garlic)", "Tomato, red onion", "Oregano, lemon"),
            listOf("Marinate chicken in lemon, oregano and garlic; grill or pan-sear.", "Slice and serve over rice with tomato, onion and tzatziki.", "Warm pita on the side.")),
        r("m_fajita", "Chicken Fajita Burrito Bowl", "🫑", MealType.MAIN, "Mexican", listOf("Chicken", "Beans & lentils"), listOf("Rice"), setOf(DAIRY), 25, 840, 58, 100, 20,
            listOf("200 g chicken breast, sliced", "1 cup cooked rice", "½ can black beans", "Peppers & onion", "Fajita seasoning", "Salsa, sour cream, cheese"),
            listOf("Sear chicken with fajita seasoning.", "Cook peppers and onion until charred.", "Build bowls with rice, beans, chicken, veg and toppings.")),
        r("m_catfish", "Cajun Catfish, Rice & Greens", "🐠", MealType.MAIN, "Southern", listOf("Fish"), listOf("Rice"), setOf(FISH), 25, 720, 46, 78, 22,
            listOf("200 g catfish fillet", "1 cup cooked rice", "2 cups collard greens", "Cajun seasoning", "1 tbsp oil", "Lemon"),
            listOf("Rub catfish with Cajun seasoning and pan-fry 3–4 minutes a side.", "Sauté greens with garlic and a splash of broth.", "Serve with rice and lemon.")),
        r("m_bbqmac", "BBQ Chicken & Mac", "🍖", MealType.MAIN, "Southern", listOf("Chicken"), listOf("Pasta"), setOf(DAIRY, GLUTEN), 35, 920, 62, 96, 30,
            listOf("220 g chicken thighs", "3 tbsp BBQ sauce", "100 g macaroni", "40 g cheddar + splash of milk", "Side of green beans"),
            listOf("Bake chicken at 200 °C / 400 °F for 25 minutes, brushing with BBQ sauce.", "Boil macaroni, stir in cheddar and milk.", "Serve with green beans.")),
        r("m_lentilpasta", "Lentil Bolognese", "🍝", MealType.MAIN, "Italian", listOf("Beans & lentils"), listOf("Pasta"), setOf(GLUTEN), 30, 720, 34, 112, 14,
            listOf("1 cup cooked brown lentils", "120 g pasta", "1 cup marinara", "Onion, carrot, garlic", "Nutritional yeast"),
            listOf("Soften onion, carrot and garlic.", "Add lentils and marinara; simmer 10 minutes.", "Toss with pasta and top with nutritional yeast.")),
        r("m_falafel", "Falafel Quinoa Bowl", "🧆", MealType.MAIN, "Mediterranean", listOf("Beans & lentils"), listOf("Quinoa"), emptySet(), 25, 690, 26, 88, 26,
            listOf("6 baked falafel", "1 cup cooked quinoa", "Hummus", "Cucumber, tomato, parsley", "Tahini-lemon drizzle"),
            listOf("Bake or air-fry falafel until crisp.", "Layer quinoa, salad and hummus.", "Top with falafel and tahini drizzle.")),
        r("m_steak", "Steak & Garlic Mash", "🥩", MealType.MAIN, "American", listOf("Beef"), listOf("Potatoes"), setOf(DAIRY), 30, 880, 60, 66, 40,
            listOf("220 g sirloin", "350 g potatoes", "1 tbsp butter + splash of milk", "2 garlic cloves", "Asparagus or broccoli"),
            listOf("Boil potatoes, mash with butter, milk and garlic.", "Sear steak 3–4 minutes a side; rest 5 minutes.", "Serve with mash and greens.")),

        // ---------- Snacks ----------
        r("s_pbj", "PB&J on Whole Wheat", "🥜", MealType.SNACK, "American", emptyList(), listOf("Bread", "Fruit"), setOf(NUTS, GLUTEN), 3, 480, 18, 58, 20,
            listOf("2 slices whole-wheat bread", "2 tbsp peanut butter", "1 tbsp jam", "Glass of milk (optional, +150 kcal)"),
            listOf("Spread, stack, eat. Add milk for extra calories.")),
        r("s_trail", "Nut & Fruit Trail Mix", "🌰", MealType.SNACK, "American", emptyList(), listOf("Fruit"), setOf(NUTS), 2, 450, 14, 40, 28,
            listOf("30 g almonds", "20 g cashews", "30 g raisins", "15 g dark chocolate chips"),
            listOf("Mix in a bag or container. Keep one in your gym bag.")),
        r("s_cottage", "Cottage Cheese & Pineapple", "🍍", MealType.SNACK, "American", listOf("Greek yogurt"), listOf("Fruit"), setOf(DAIRY), 3, 320, 30, 30, 8,
            listOf("250 g cottage cheese", "½ cup pineapple chunks", "Drizzle of maple syrup"),
            listOf("Combine in a bowl and enjoy.")),
        r("s_ricecakes", "Rice Cakes, Almond Butter & Banana", "🍌", MealType.SNACK, "American", emptyList(), listOf("Rice", "Fruit"), setOf(NUTS), 3, 380, 10, 48, 18,
            listOf("3 rice cakes", "1½ tbsp almond butter", "1 banana, sliced", "Cinnamon"),
            listOf("Spread almond butter on rice cakes and top with banana and cinnamon.")),
        r("s_hummus", "Hummus, Pita & Veggies", "🫓", MealType.SNACK, "Mediterranean", listOf("Beans & lentils"), listOf("Bread"), setOf(GLUTEN), 5, 420, 14, 52, 18,
            listOf("100 g hummus", "1 large pita", "Carrot and cucumber sticks"),
            listOf("Warm pita, slice into wedges, and dip with veg.")),
        r("s_jerky", "Beef Jerky & Apple", "🍎", MealType.SNACK, "American", listOf("Beef"), listOf("Fruit"), setOf(SOY), 1, 300, 30, 34, 4,
            listOf("50 g beef jerky", "1 apple"),
            listOf("Grab and go.")),
        r("s_edamame", "Salted Edamame & Rice Crackers", "🫛", MealType.SNACK, "Asian", listOf("Tofu"), listOf("Rice"), setOf(SOY), 5, 360, 22, 38, 12,
            listOf("1½ cups edamame in pods", "30 g rice crackers", "Flaky salt"),
            listOf("Microwave edamame 3 minutes, salt, and serve with crackers.")),

        // ---------- Shakes ----------
        r("sh_mass", "Homemade Mass Gainer", "🥤", MealType.SHAKE, "American", listOf("Whey"), listOf("Oats", "Fruit"), setOf(DAIRY, NUTS), 5, 1000, 60, 120, 30,
            listOf("400 ml whole milk", "2 scoops whey", "60 g oats", "1 banana", "2 tbsp peanut butter", "1 tbsp honey or maple syrup"),
            listOf("Blend everything for 45 seconds. Sip it over 30 minutes if it's a lot.")),
        r("sh_chocpb", "Chocolate PB Recovery Shake", "🍫", MealType.SHAKE, "American", listOf("Whey"), listOf("Fruit"), setOf(DAIRY, NUTS), 5, 680, 50, 62, 24,
            listOf("350 ml milk", "1½ scoops chocolate whey", "1 frozen banana", "1½ tbsp peanut butter", "Ice"),
            listOf("Blend until smooth. Great within an hour after training.")),
        r("sh_vegan", "Vegan Berry Power Shake", "🫐", MealType.SHAKE, "American", listOf("Plant protein"), listOf("Oats", "Fruit"), emptySet(), 5, 640, 40, 82, 16,
            listOf("400 ml oat milk", "2 scoops pea protein", "1 cup frozen berries", "40 g oats", "1 tbsp ground flax", "1 tbsp maple syrup"),
            listOf("Blend for 45 seconds until thick and smooth.")),
        r("sh_coffee", "Iced Coffee Protein Shake", "☕", MealType.SHAKE, "American", listOf("Whey"), listOf("Oats"), setOf(DAIRY), 5, 520, 44, 56, 12,
            listOf("1 shot espresso or ½ cup cold brew", "300 ml milk", "1½ scoops vanilla whey", "30 g oats", "Ice"),
            listOf("Blend everything with ice. A great breakfast-on-the-go."))
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
}

object MealPlanner {
    private val meats = setOf("Chicken", "Beef", "Turkey", "Pork")
    private val seafood = setOf("Fish", "Shellfish")
    private val animal = setOf("Eggs", "Greek yogurt", "Whey")

    fun allowed(r: Recipe, f: FoodPrefs): Boolean {
        val dietOk = when (f.diet) {
            Diet.NONE -> true
            Diet.HALAL -> "Pork" !in r.proteins
            Diet.PESCATARIAN -> r.proteins.none { it in meats }
            Diet.VEGETARIAN -> r.proteins.none { it in meats || it in seafood }
            Diet.VEGAN -> r.proteins.none { it in meats || it in seafood || it in animal } && DAIRY !in r.allergens && EGGS !in r.allergens
        }
        if (!dietOk) return false
        if (r.allergens.any { it in f.allergies }) return false
        val dislikes = f.dislikes.lowercase().split(',', ';', '\n').map { it.trim() }.filter { it.length >= 3 }
        if (dislikes.isNotEmpty()) {
            val text = (r.name + " " + r.ingredients.joinToString(" ")).lowercase()
            if (dislikes.any { it in text }) return false
        }
        return true
    }

    fun score(r: Recipe, p: Profile): Int {
        val f = p.food
        var s = r.proteins.count { it in f.proteins } * 3 + r.carbs.count { it in f.carbs } * 2
        if (r.cuisine in f.cuisines) s += 2
        if (r.id in p.favorites) s += 4
        if (r.minutes > f.cookTime.maxMin) s -= 4
        if (f.appetite == Appetite.SMALL && r.type == MealType.SHAKE) s += 3
        if (f.appetite == Appetite.BIG && r.kcal >= 800) s += 2
        return s
    }

    fun reason(r: Recipe, p: Profile): String {
        val likes = (r.proteins.filter { it in p.food.proteins } + r.carbs.filter { it in p.food.carbs }).map { it.lowercase() }
        return when {
            r.id in p.favorites -> "One of your favorites"
            likes.size >= 2 -> "You like ${likes[0]} & ${likes[1]}"
            likes.size == 1 -> "You like ${likes[0]}"
            r.cuisine in p.food.cuisines -> "${r.cuisine} — a cuisine you picked"
            r.type == MealType.SHAKE -> "Easy liquid calories"
            else -> "High-protein pick"
        }
    }

    fun match(r: Recipe, p: Profile): Int {
        val raw = score(r, p).coerceAtLeast(0)
        return (55 + raw * 5).coerceAtMost(99)
    }

    private fun slots(f: FoodPrefs): List<Pair<String, Set<MealType>>> {
        val b = "Breakfast" to setOf(MealType.BREAKFAST)
        val l = "Lunch" to setOf(MealType.MAIN)
        val d = "Dinner" to setOf(MealType.MAIN)
        val snackTypes = if (f.appetite == Appetite.SMALL) setOf(MealType.SHAKE, MealType.SNACK) else setOf(MealType.SNACK, MealType.SHAKE)
        val s = "Snack" to snackTypes
        val shake = "Shake" to setOf(MealType.SHAKE)
        return when (f.mealsPerDay.coerceIn(3, 6)) {
            3 -> listOf(b, l, d)
            4 -> listOf(b, l, s, d)
            5 -> listOf(b, s, l, if (f.appetite == Appetite.SMALL) shake else s, d)
            else -> listOf(b, s, l, shake, d, s)
        }
    }

    fun plan(p: Profile, calories: Int): List<PlannedMeal> {
        val ok = Recipes.all.filter { allowed(it, p.food) }
        if (ok.isEmpty()) return emptyList()
        val seed = LocalDate.now().dayOfYear + p.planShuffle * 3
        val used = mutableSetOf<String>()
        val picks = slots(p.food).mapIndexedNotNull { i, (slot, types) ->
            val candidates = ok.filter { it.type in types && it.id !in used }
                .sortedWith(compareByDescending<Recipe> { score(it, p) }.thenBy { it.id })
                .ifEmpty { ok.filter { it.id !in used } }
            if (candidates.isEmpty()) return@mapIndexedNotNull null
            val top = candidates.take(maxOf(3, candidates.size / 3))
            val pick = top[(seed + i * 7).mod(top.size)]
            used += pick.id
            slot to pick
        }
        val total = picks.sumOf { it.second.kcal }.coerceAtLeast(1)
        val factor = calories.toDouble() / total
        return picks.map { (slot, r) ->
            val servings = ((factor * 4).roundToInt() / 4.0).coerceIn(0.5, 3.0)
            PlannedMeal(slot, r, servings, reason(r, p))
        }
    }
}
