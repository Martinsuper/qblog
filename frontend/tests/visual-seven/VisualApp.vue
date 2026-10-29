<script setup>
import {ref,reactive,computed} from 'vue'
import KnowledgeSeven from '../../src/components/KnowledgeSeven.vue'
import fixture from './fixture.json'
const active=ref(fixture.notes.find(n=>n.id===1)),draft=reactive({...active.value}),collapsed=reactive(new Set())
const flat=fixture.folders.flatMap(f=>[{...f,path:f.name,depth:0,hasChildren:true},...f.children.map(c=>({...c,path:f.name+' / '+c.name,depth:1,hasChildren:false}))])
const visible=computed(()=>flat.filter(f=>!f.parentId||!collapsed.has(f.parentId)))
const folderName=id=>flat.find(f=>f.id===id)?.path||'收件箱'
</script>
<template><KnowledgeSeven color="light" :folders="fixture.folders" :flat-folders="visible" :all-folders="flat" :collapsed="collapsed" :counts="{all:48,inbox:8,favorites:5}" :notes="fixture.notes" scope="all" scope-title="全部笔记" search="" tag="" source="" sort="updated" :active="active" :draft="draft" mobile-view="reader" :total="48" :page="1" :page-size="50" save-status="直接编辑 · 停顿后自动保存" :folder-name="folderName" @collapse="collapsed.has($event)?collapsed.delete($event):collapsed.add($event)"/></template>
