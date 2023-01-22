<template id="settings">
  <navbar active="settings"></navbar>
  <main class="container">
    <h3 class="mt-4 mb-3">Settings</h3>
    <div class="mb-3">
      <label for="maxAgeDays" class="form-label">Maximum news age in days to fetch</label>
      <input type="number" class="form-control" id="maxAgeDays" v-model="settings.maxAgeDays">
    </div>
    <div class="mb-3">
      <label for="maxCount" class="form-label">Maximum news to fetch</label>
      <input type="number" class="form-control" id="maxCount" v-model="settings.maxCount">
    </div>
    <div class="mb-3">
      <label for="sortBy" class="form-label">Sort news by field</label>
      <select class="form-select" id="sortBy" v-model="settings.sortBy">
        <option value="score" :selected="settings.sortBy === 'score'">score</option>
        <option value="commentsCount" :selected="settings.sortBy === 'commentsCount'">commentsCount</option>
        <option value="date" :selected="settings.sortBy === 'date'">date</option>
      </select>
    </div>
    <hr class="my-4"/>
    <div class="mb-3">
      <label for="maxAgeDaysSearch" class="form-label">Maximum news age in days to search or recommend</label>
      <input type="number" class="form-control" id="maxAgeDaysSearch" v-model="settings.maxAgeDaysSearch">
    </div>
    <div class="mb-3">
      <label for="maxCountSearch" class="form-label">Maximum news to search or recommend</label>
      <input type="number" class="form-control" id="maxCountSearch" v-model="settings.maxCountSearch">
    </div>
    <div class="mb-3">
      <label for="favoriteNews" class="form-label">Favorite news</label>
      <textarea class="form-control" id="favoriteNews" rows="8" :value="getFavorites" @input="updateFavorites"></textarea>
    </div>
  </main>
</template>
<script>
app.component("settings", {
  template: "#settings",
  data: function () {
    return {
      settings: loadSettings(),
      favorites: loadFavorites()
    }
  },
  watch: {
    settings: {
      handler(newValue) {
        localStorage.setItem("settings", JSON.stringify(newValue));
      },
      deep: true
    }
  },
  computed: {
    getFavorites() {
      return JSON.stringify(this.favorites, null, 2);
    }
  },
  methods: {
    updateFavorites(event) {
      localStorage.setItem("favorites", event.target.value);
    }
  }
});
</script>
