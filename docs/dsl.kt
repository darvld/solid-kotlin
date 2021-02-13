
class MainFragment : Fragment {
	override fun createUI(container: Container) = container.column {
		header("Title")
		header("Subtitle", h=2)

		row {
			image("files/logo.png")

			column {
				subtitle("App name")
				label("Summary")
			}
		}

		row {
			primaryButton("Ok").onClick { 
				toast("User agreed, doing something...")
			}

			textButton("Cancel").onClick {
				toast("User didn't agree, doing something anyway...")
			}
		}
	}

}