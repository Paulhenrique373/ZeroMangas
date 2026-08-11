package com.example.zeromangas.repository

import com.example.zeromangas.data.model.Manga

class MangaRepository {

    fun listarMangas(): List<Manga> {
        return listOf(
            Manga(
                id = "1",
                nome = "One Piece Vol. 1",
                marca = "Panini",
                categoria = "Shounen",
                volume = 1,
                preco = 29.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/cvr9781569319017_9781569319017_hr.jpg",
                descricao = "A jornada de Monkey D. Luffy em busca do maior tesouro dos mares começa aqui.",
                emDestaque = true
            ),
            Manga(
                id = "2",
                nome = "Naruto Vol. 1",
                marca = "Panini",
                categoria = "Shounen",
                volume = 1,
                preco = 27.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/cvr9781569319000_9781569319000_hr.jpg",
                descricao = "Um jovem ninja busca reconhecimento e sonha em se tornar Hokage.",
                emDestaque = true
            ),
            Manga(
                id = "3",
                nome = "Bleach Vol. 1",
                marca = "MPEG",
                categoria = "Shounen",
                volume = 1,
                preco = 27.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/cvr9781591164418_9781591164418_hr.jpg",
                descricao = "Ichigo Kurosaki ganha poderes de Shinigami e passa a proteger os vivos dos espíritos malignos."
            ),
            Manga(
                id = "4",
                nome = "Jujutsu Kaisen Vol. 1",
                marca = "Devir",
                categoria = "Shounen",
                volume = 1,
                preco = 32.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/onix/cvr9781974710027/jujutsu-kaisen-vol-1-9781974710027_lg.jpg",
                descricao = "Yuji Itadori se envolve em um mundo de feiticeiros e maldições ao engolir um dedo amaldiçoado.",
                emDestaque = true
            ),
            Manga(
                id = "5",
                nome = "Demon Slayer Vol. 1",
                marca = "Panini",
                categoria = "Shounen",
                volume = 1,
                preco = 29.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/onix/cvr9781974700523/demon-slayer-kimetsu-no-yaiba-vol-1-9781974700523_hr.jpg",
                descricao = "Tanjiro luta para curar sua irmã transformada em demônio e vingar sua família."
            ),
            Manga(
                id = "6",
                nome = "Tokyo Ghoul Vol. 1",
                marca = "JBC",
                categoria = "Seinen",
                volume = 1,
                preco = 34.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/onix/cvr9781421580364/tokyo-ghoul-vol-1-9781421580364_hr.jpg",
                descricao = "Kaneki se torna metade ghoul após um encontro fatal e precisa aprender a conviver com sua nova natureza."
            ),
            Manga(
                id = "7",
                nome = "Berserk Vol. 1",
                marca = "Devir",
                categoria = "Seinen",
                volume = 1,
                preco = 39.90,
                imagemUrl = "https://i.gr-assets.com/images/S/compressed.photo.goodreads.com/books/1501000017l/248871._SX318_.jpg",
                descricao = "A saga sombria de Guts, um guerreiro solitário em um mundo brutal e implacável.",
                emDestaque = true
            ),
            Manga(
                id = "8",
                nome = "Vinland Saga Vol. 1",
                marca = "NewPOP",
                categoria = "Seinen",
                volume = 1,
                preco = 44.90,
                imagemUrl = "https://i.gr-assets.com/images/S/compressed.photo.goodreads.com/books/1350285863l/7787959.jpg",
                descricao = "Thorfinn busca vingança em meio às invasões vikings na Inglaterra medieval."
            ),
            Manga(
                id = "9",
                nome = "Horimiya Vol. 1",
                marca = "JBC",
                categoria = "Romance",
                volume = 1,
                preco = 26.90,
                imagemUrl = "https://i.gr-assets.com/images/S/compressed.photo.goodreads.com/books/1727083089l/16155151._SX318_.jpg",
                descricao = "Duas vidas escolares opostas se cruzam e revelam lados que ninguém esperava conhecer."
            ),
            Manga(
                id = "10",
                nome = "Kaguya-sama Vol. 1",
                marca = "Pipoca & Nanquim",
                categoria = "Romance",
                volume = 1,
                preco = 28.90,
                imagemUrl = "https://d28hgpri8am2if.cloudfront.net/book_images/onix/cvr9781974700301/kaguya-sama-love-is-war-vol-1-9781974700301_xlg.jpg",
                descricao = "Uma guerra de orgulho entre dois gênios que estão apaixonados, mas se recusam a admitir.",
                emDestaque = true
            )
        )
    }

    fun listarCategorias(): List<String> {
        return listOf("Shounen", "Seinen", "Romance")
    }

    fun listarMarcas(): List<String> {
        return listOf("Panini", "JBC", "NewPOP", "Devir", "MPEG", "Pipoca & Nanquim")
    }
}