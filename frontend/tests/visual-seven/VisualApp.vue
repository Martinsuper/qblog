<script setup>
import KnowledgeSeven from '../../src/components/KnowledgeSeven.vue'
import fixture from './fixture.json'
import { createVisualModel } from './model.js'

const model = createVisualModel(fixture)
const { state, folders, active, counts, folderCounts, visibleWithCounts, folderName, scopeTitle, filtered, pageNotes } = model
function scope(value) { model.setFilter('scope', value); state.mobileView = 'list' }
function filterTag(value) { model.setFilter('tag', value); state.mobileView = 'list' }
</script>

<template>
  <KnowledgeSeven
    color="light"
    :folders="folders"
    :flat-folders="visibleWithCounts"
    :all-folders="folderCounts"
    :collapsed="state.collapsed"
    :counts="counts"
    :notes="pageNotes"
    :scope="state.scope"
    :scope-title="scopeTitle"
    :search="state.search"
    :list-search="state.listSearch"
    :tag="state.tag"
    :source="state.source"
    :sort="state.sort"
    :active="active"
    :draft="state.draft"
    :mobile-view="state.mobileView"
    :total="filtered.length"
    :page="state.page"
    :page-size="state.pageSize"
    save-status="仅示例数据 · 修改保存在内存中"
    :folder-name="folderName"
    @scope="scope"
    @search="model.setFilter('search', $event)"
    @list-search="model.setFilter('listSearch', $event)"
    @tag="model.setFilter('tag', $event)"
    @source="model.setFilter('source', $event)"
    @sort="state.sort = state.sort === 'title' ? 'updated' : 'title'; state.page = 1"
    @view="state.mobileView = $event"
    @collapse="state.collapsed.has($event) ? state.collapsed.delete($event) : state.collapsed.add($event)"
    @collapse-all="model.collapseAll()"
    @open-note="model.openNote($event)"
    @dirty="model.dirty()"
    @favorite="model.favorite()"
    @new-note="model.newNote()"
    @delete="model.removeNote()"
    @filter-tag="filterTag"
    @page="state.page = Math.max(1, Math.min(Math.ceil(filtered.length / state.pageSize), state.page + $event))"
  />
</template>
