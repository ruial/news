<template id="news-detail">
  <navbar></navbar>

  <main class="container">
    <div class="my-3 p-3 bg-body rounded shadow-sm">

      <p class="pt-3" v-if="news.loading">Loading news...</p>
      <p class="pt-3" v-if="news.loadError">Failed to load news! ({{news.loadError.text}})</p>
      
      <div v-if="news.data.id">
        <h5 class="border-bottom pb-2 mb-0">
          <a class="title" :href="news.data.storyUrl" rel="nofollow">{{ news.data.title }}</a>
        </h5>

        <div class="d-flex text-muted pt-3 border-bottom">
          <p class="pb-3 mb-0 small lh-sm">
            {{ formatDate(news.data.date) }}
            <span class="d-block">{{ news.data.score }} points - {{ news.data.commentsCount }} <a :href="news.data.commentsUrl" rel="nofollow">comments</a></span>
          </p>
        </div>
    
        <div class="content" v-html="content"></div>

        <small class="links d-flex flex-wrap justify-content-between mt-4">
          <a :href="news.data.storyUrl" rel="nofollow">Original article</a>
          <a href="#" @click.prevent="toggleReader">Toggle reader mode</a>
          <a href="#" @click.prevent="toggleFavorite"> {{ isFavorite ? "Remove" : "Add" }} favorite</a>
          <a :href="news.data.commentsUrl" rel="nofollow">All comments</a>
        </small>
      </div>
    </div>

    <div v-if="news.data.id">
      <button id="recos" type="button" class="btn btn-primary w-100 mt-2 mb-4" @click="getRecos">Recommend similar</button>
      <p class="pb-3" v-if="recos.loading">Loading news...</p>
      <p class="pb-3" v-if="recos.loadError">Failed to load news! ({{recos.loadError.text}})</p>
      <p class="pb-3" v-if="!recos.loading && !recos.loadError && recos.data.length === 0">No recommendations to show.</p>
      <searchresults :results="recos.data" v-if="!recos.loading && recos.data.length > 0"></searchresults>
    </div>

  </main>
</template>
<script>
app.component("news-detail", {
  template: "#news-detail",
  data: function() {
    return {
      news: {
        loading: true,
        loadError: null,
        data: {}
      },
      recos: {
        loading: false,
        loadError: null,
        data: {}
      },
      readableContent: "",
      readerEnabled: true,
      settings: loadSettings(),
      favorites: loadFavorites()
    }
  },
  created() {
    const parts = document.URL.split("/");
    // handle potential trailing slash, part already encoded
    const newsId = parts.pop() || parts.pop();
    fetch(`/api/news/${newsId}`).then(res => {
      if (res.ok) return res.json();
      return res.text().then(text => { throw new Error(text) });
    })
      .then(data => {
        this.news.data = data;
        const parser = new DOMParser();
        const doc = parser.parseFromString(data.content, "text/html");
        const article = new Readability(doc).parse();
        this.readableContent = article.content;
      })
      .catch(error => this.news.loadError = { "text": error })
      .finally(() => this.news.loading = false);
  },
  computed: {
    isFavorite() {
      return this.favorites.map(n => n.id).includes(this.news.data.id);
    },
    content() {
      return this.readerEnabled ? this.readableContent : this.news.data.content;
    }
  },
  methods: {
    toggleFavorite() {
      const isFavorite = this.isFavorite;
      // refresh in case favorites were updated in other browser tab
      this.favorites = loadFavorites();
      if (isFavorite) {
        this.favorites = this.favorites.filter(n => n.id !== this.news.data.id);
      } else {
        const { id , title, storyUrl, commentsUrl } = this.news.data;
        this.favorites.push({ id, title, storyUrl, commentsUrl });
      }
      localStorage.setItem("favorites", JSON.stringify(this.favorites));
    },
    toggleReader() {
      this.readerEnabled = !this.readerEnabled;
      setTimeout(function() {
        window.scrollTo({ top: 0, behavior: "smooth" });
      }, 10);
    },
    getRecos() {
      this.recos.loading = true;
      const { maxCountSearch, maxAgeDaysSearch } = this.settings;
      const params = buildQueryParams(maxCountSearch, maxAgeDaysSearch); 
      fetch(`/api/recommendations/news?${params}`, {
        method: "POST",
        headers: {
          "Accept": "application/json",
          "Content-Type": "application/json"
        },
        body: JSON.stringify([this.news.data.id])
      }).then(res => {
        if (res.ok) return res.json();
        return res.text().then(text => { throw new Error(text) });
      })
        .then(data => {
          this.recos.data = data;
          setTimeout(() => document.getElementById("recos").scrollIntoView(), 50);
        })
        .catch(error => this.recos.loadError = { "text": error })
        .finally(() => this.recos.loading = false);
      }
  }
});
</script>
<style>
  .title {
    color: #212529;
    text-decoration: none;
  }
  .links {
    gap: 10px;
  }
  .content img {
    max-width: 100%;
    height: auto;
  }
</style>
