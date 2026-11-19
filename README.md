# Castle Crashers V1 — Android

Port Android baseado no runtime Winlator integrado ao próprio APK. O aplicativo importa uma instalação Windows fornecida pelo usuário e tenta executá-la em um contêiner Wine/Box64 ARM64.

## V1.0.0

- Winlator integrado no mesmo APK (não exige instalar o Winlator separadamente).
- Importação da pasta completa ou de um ZIP do jogo por seletor de documentos Android, com validação PE x86, pasta `data`, DLLs necessárias e proteção contra caminhos ZIP maliciosos.
- Verificação PE para confirmar castle.exe Windows x86/32-bit.
- Criação do contêiner Wine e cópia dos arquivos para o disco C:.
- Configurações de resolução, áudio, DXVK/WineD3D e drivers gráficos, com perfil automático conservador em aparelhos com até 3 GB de RAM e seleção de preset Box64.
- HUD integrado de FPS, uso percentual de CPU, frequência do processador e RAM, ativado por padrão.
- Controles virtuais Xbox e suporte do runtime a controles externos, teclado e mouse.
- Descoberta de salas LAN experimental.
- Identidade de pacote e autoridade FileProvider próprias, para coexistir com o Winlator instalado e evitar conflitos de instalação.

O APK não inclui Castle.exe nem arquivos comerciais do jogo. Importe uma cópia do jogo que você tenha direito de usar. O arquivo ZIP fornecido para análise não será redistribuído no APK ou no repositório.

## Download

A Release Castle Crashers V1 publica um único arquivo instalável:
Castle-Crashers-v1.0.0-arm64.apk.

O workflow verifica a presença do runtime integrado, das bibliotecas ARM64 e da assinatura antes de publicar. A identidade de assinatura de debug é mantida no cache do CI para reduzir falhas de atualização por conflito de assinatura. O APK de teste pequeno não será anexado à Release.

## Interface e arte

A interface está preparada para receber o banner/ícone oficial ou fan art com permissão de uso. Não será usada arte gerada artificialmente nem obra de fã sem licença/atribuição. As imagens que o usuário ainda vai enviar serão integradas na revisão visual seguinte.

## Limitações conhecidas

- A compilação e a inclusão do runtime podem ser verificadas em CI; isso não substitui o teste real do jogo em um telefone.
- O primeiro início precisa extrair o rootfs Wine e criar o prefixo, exigindo espaço livre considerável.
- LAN atualmente é apenas descoberta de salas, não sincroniza uma partida.
- Steam friends, convites, matchmaking e Steamworks online não estão implementados.
- O suporte gráfico varia conforme GPU/driver. No Moto G7 Play/Adreno 506, teste WineD3D/OpenGL caso DXVK/Vulkan apresente falhas.

## Compilar

Requisitos: JDK 17, Android SDK 35, NDK 24.0.8215888, CMake 3.22.1 e Git com submódulos.

    git clone --recurse-submodules https://github.com/crazybreakdown885-boop/Castle.git
    cd Castle
    gradle --no-daemon :app:assembleDebug

A integração usa o código Winlator upstream fixado como submódulo. Consulte THIRD_PARTY_NOTICES.md para licenças e créditos.


O build da branch principal é publicado somente após as verificações do APK ARM64 completo.


As pull requests verificam o mesmo runtime ARM64, tamanho do APK, bibliotecas nativas, licenças e assinatura exigidos no build de release. A publicação anexa um único APK ARM64 e seu checksum SHA-256.
