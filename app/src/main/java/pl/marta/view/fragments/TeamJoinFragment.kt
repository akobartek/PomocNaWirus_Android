package pl.marta.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.content_team_join.view.*
import kotlinx.android.synthetic.main.fragment_team_join.view.*
import pl.marta.R
import pl.marta.model.Team
import pl.marta.utils.showBasicAlertDialog
import pl.marta.utils.showShortToast
import pl.marta.utils.tryToRunFunctionOnInternet
import pl.marta.view.activities.MainActivity
import pl.marta.viewmodel.TeamsViewModel

class TeamJoinFragment : Fragment() {

    private lateinit var mViewModel: TeamsViewModel
    private lateinit var mLoadingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_team_join, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inflateToolbarMenu()

        mViewModel = ViewModelProvider(requireActivity()).get(TeamsViewModel::class.java)
        mLoadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()

        view.browseTeamsBtn.setOnClickListener {
            findNavController().navigate(TeamJoinFragmentDirections.showTeamFindFragment())
        }

        view.createTeamBtn.setOnClickListener {
            val createTeamBottomSheet = TeamCreateBottomSheetFragment(this@TeamJoinFragment)
            createTeamBottomSheet.show(childFragmentManager, createTeamBottomSheet.tag)
        }

        view.joinTeamBtn.setOnClickListener {
            val teamCode = view.teamCodeET.text.toString().trim()
            if (teamCode.isEmpty()) {
                view.teamCodeET.error = getString(R.string.team_code_empty_error)
                view.teamCodeET.requestFocus()
                return@setOnClickListener
            }
            mLoadingDialog.show()
            requireActivity().tryToRunFunctionOnInternet({
                mViewModel.addUserToTeam(teamCode)
                    .observe(viewLifecycleOwner, Observer { addedToTeam ->
                        if (mLoadingDialog.isShowing) mLoadingDialog.hide()
                        if (addedToTeam) {
                            MainActivity.subscribeToNewOrdersNotifications(teamCode)
                            requireContext().showShortToast(R.string.added_to_team_successful)
                            findNavController().navigate(
                                TeamJoinFragmentDirections.showTaskListFragment(teamCode)
                            )
                        } else requireContext().showBasicAlertDialog(
                            R.string.team_not_found,
                            R.string.team_code_invalid_error
                        )
                    })
            }, {
                if (mLoadingDialog.isShowing) mLoadingDialog.hide()
            })
        }
    }

    override fun onStop() {
        super.onStop()
        if (mLoadingDialog.isShowing) mLoadingDialog.hide()
    }

    fun createNewTeam(team: Team) {
        mLoadingDialog.show()
        requireActivity().tryToRunFunctionOnInternet({
            mViewModel.createNewTeam(team).observe(viewLifecycleOwner, Observer { teamId ->
                if (teamId.isNotEmpty()) {
                    if (mLoadingDialog.isShowing) mLoadingDialog.hide()
                    MainActivity.subscribeToNewOrdersNotifications(teamId)
                    requireContext().showShortToast(R.string.team_created_successful)
                    findNavController().navigate(
                        TeamJoinFragmentDirections.showOrdersListFragment(teamId)
                    )
                } else requireContext().showBasicAlertDialog(
                    R.string.update_error_title,
                    R.string.update_error_message
                )
            })
        }, {
            if (mLoadingDialog.isShowing) mLoadingDialog.hide()
        })
    }

    private fun inflateToolbarMenu() {
        view?.teamJoinToolbar?.inflateMenu(R.menu.team_join_menu)
        view?.teamJoinToolbar?.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_settings -> {
                    findNavController().navigate(TeamJoinFragmentDirections.showSettingsFragment())
                    true
                }
                R.id.action_sign_out -> {
                    (requireActivity() as MainActivity).signOut()
                    true
                }
                else -> true
            }
        }
    }
}
